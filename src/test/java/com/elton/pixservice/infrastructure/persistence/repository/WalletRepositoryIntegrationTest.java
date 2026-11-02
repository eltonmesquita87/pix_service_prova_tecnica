package com.elton.pixservice.infrastructure.persistence.repository;

import com.elton.pixservice.domain.entity.Wallet;
import com.elton.pixservice.domain.valueobject.Cpf;
import com.elton.pixservice.domain.valueobject.Money;
import com.elton.pixservice.domain.valueobject.WalletHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(WalletRepositoryImpl.class)
@Tag("docker")
@DisplayName("WalletRepository Integration Tests (real Postgres)")
class WalletRepositoryIntegrationTest {

    private static final String CPF_A = "12345678909";
    private static final String CPF_B = "52998224725";

    @Autowired
    private WalletRepositoryImpl walletRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Wallet newWallet(String userId, WalletHolder holder) {
        return Wallet.builder()
                .userId(userId)
                .balance(Money.zero())
                .holder(holder)
                .build();
    }

    private Wallet saveAndReload(Wallet wallet) {
        Wallet saved = walletRepository.save(wallet);
        entityManager.flush();
        entityManager.clear();
        return walletRepository.findById(saved.getId()).orElseThrow();
    }

    @Test
    @DisplayName("WHOLD-15: Flyway applies V1 to V3 and Hibernate validate passes")
    void flywayAppliesV1ToV3AndHibernateValidatePasses() {
        // The context only starts when ddl-auto=validate accepts the entity against the migrated schema.
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND version IN ('1', '2', '3')",
                Integer.class);
        Integer columns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'wallets' "
                        + "AND column_name IN ('cpf', 'nome', 'email', 'telefone') AND is_nullable = 'YES'",
                Integer.class);

        assertEquals(3, applied);
        assertEquals(4, columns);
    }

    @Test
    @DisplayName("WHOLD-03: a wallet with all four holder fields round trips and stores only the CPF digits")
    void roundTripWithAllHolderFields() {
        WalletHolder holder = WalletHolder.of("123.456.789-09", "Maria Silva", "maria@example.com", "+5511999999999");

        Wallet reloaded = saveAndReload(newWallet("user-1", holder));

        assertEquals(holder, reloaded.getHolder());
        assertEquals("12345678909", reloaded.getHolder().getCpf().getDigits());
        assertEquals("Maria Silva", reloaded.getHolder().getNome());
        assertEquals("maria@example.com", reloaded.getHolder().getEmail());
        assertEquals("+5511999999999", reloaded.getHolder().getTelefone());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT cpf, nome, email, telefone FROM wallets WHERE id = ?", reloaded.getId());
        assertEquals("12345678909", row.get("cpf"));
        assertEquals("Maria Silva", row.get("nome"));
        assertEquals("maria@example.com", row.get("email"));
        assertEquals("+5511999999999", row.get("telefone"));
    }

    @Test
    @DisplayName("WHOLD-13: a wallet without holder round trips with an empty holder and null columns")
    void roundTripWithoutHolder() {
        Wallet reloaded = saveAndReload(newWallet("user-2", WalletHolder.empty()));

        assertEquals(WalletHolder.empty(), reloaded.getHolder());
        assertNull(reloaded.getHolder().getCpf());
        assertNull(reloaded.getHolder().getNome());
        assertNull(reloaded.getHolder().getEmail());
        assertNull(reloaded.getHolder().getTelefone());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT cpf, nome, email, telefone FROM wallets WHERE id = ?", reloaded.getId());
        assertNull(row.get("cpf"));
        assertNull(row.get("nome"));
        assertNull(row.get("email"));
        assertNull(row.get("telefone"));
    }

    @Test
    @DisplayName("WHOLD-10: existsByCpf is true for a stored CPF and false for another")
    void existsByCpfTrueAndFalse() {
        walletRepository.save(newWallet("user-3", WalletHolder.of(CPF_A, null, null, null)));
        entityManager.flush();

        assertTrue(walletRepository.existsByCpf(Cpf.of(CPF_A)));
        assertTrue(walletRepository.existsByCpf(Cpf.of("123.456.789-09")));
        assertFalse(walletRepository.existsByCpf(Cpf.of(CPF_B)));
    }

    @Test
    @DisplayName("WHOLD-11: two wallets with the same CPF are rejected by uk_wallets_cpf")
    void duplicateCpfIsRejectedByUniqueIndex() {
        walletRepository.save(newWallet("user-4", WalletHolder.of(CPF_A, null, null, null)));
        entityManager.flush();

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () -> {
            walletRepository.save(newWallet("user-5", WalletHolder.of(CPF_A, null, null, null)));
            entityManager.flush();
        });

        assertTrue(ex.getMessage().contains("uk_wallets_cpf"), ex.getMessage());
    }

    @Test
    @DisplayName("WHOLD-12: several wallets without CPF are accepted")
    void severalWalletsWithoutCpfAreAccepted() {
        Wallet first = walletRepository.save(newWallet("user-6", WalletHolder.empty()));
        Wallet second = walletRepository.save(newWallet("user-6", WalletHolder.empty()));
        Wallet third = walletRepository.save(newWallet("user-7", WalletHolder.of(null, "Maria Silva", null, null)));
        entityManager.flush();

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotNull(third.getId());
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallets WHERE cpf IS NULL AND id IN (?, ?, ?)", Integer.class,
                first.getId(), second.getId(), third.getId()));
    }

    @Test
    @DisplayName("WHOLD-11: a raw insert with cpf = '123' is rejected by ck_wallets_cpf_format")
    void rawInsertWithMalformedCpfIsRejectedByCheckConstraint() {
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("INSERT INTO wallets (user_id, cpf) VALUES (?, ?)", "user-8", "123"));

        assertTrue(ex.getMessage().contains("ck_wallets_cpf_format"), ex.getMessage());
    }

    @Test
    @DisplayName("WHOLD-15: a legacy row without the new columns is read with an empty holder and unchanged data")
    void legacyRowIsReadWithEmptyHolder() {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO wallets (user_id, balance, version) VALUES (?, ?, ?) RETURNING id",
                Long.class, "legacy-user", new BigDecimal("150.50"), 7L);

        Wallet wallet = walletRepository.findById(id).orElseThrow();

        assertEquals(id, wallet.getId());
        assertEquals("legacy-user", wallet.getUserId());
        assertEquals(Money.of(new BigDecimal("150.50")), wallet.getBalance());
        assertEquals(7L, wallet.getVersion());
        assertNotNull(wallet.getHolder());
        assertEquals(WalletHolder.empty(), wallet.getHolder());
    }
}
