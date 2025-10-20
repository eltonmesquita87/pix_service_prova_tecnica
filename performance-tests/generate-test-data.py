#!/usr/bin/env python3
"""
Script para gerar massas de dados para testes de performance
Gera 200 registros únicos para cada tipo de dado
"""

import csv
import random
import uuid
from datetime import datetime, timedelta

# Configurações
NUM_RECORDS = 200
NUM_TRANSFERS = 2000  # expandido: mais registros únicos para transferências
BASE_DIR = "src/test/resources/data"

def generate_wallets():
    """Gera 200 userIds únicos"""
    with open(f'{BASE_DIR}/wallets.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['userId'])
        for i in range(1, NUM_RECORDS + 1):
            writer.writerow([f'user{i:03d}'])
    print(f"✓ wallets.csv: {NUM_RECORDS} registros")

def generate_deposits():
    """Gera 200 valores de depósito"""
    with open(f'{BASE_DIR}/deposits.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['walletId', 'amount'])
        for i in range(1, NUM_RECORDS + 1):
            amount = round(random.uniform(500.0, 3000.0), 2)
            writer.writerow([i, f'{amount:.2f}'])
    print(f"✓ deposits.csv: {NUM_RECORDS} registros")

def generate_withdrawals():
    """Gera 200 valores de saque"""
    with open(f'{BASE_DIR}/withdrawals.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['walletId', 'amount'])
        for i in range(1, NUM_RECORDS + 1):
            amount = round(random.uniform(25.0, 150.0), 2)
            writer.writerow([i, f'{amount:.2f}'])
    print(f"✓ withdrawals.csv: {NUM_RECORDS} registros")

def generate_pix_keys():
    """Gera 200 chaves Pix variadas (CPF, EMAIL, PHONE, EVP)"""
    key_types = ['CPF', 'EMAIL', 'PHONE', 'EVP']

    with open(f'{BASE_DIR}/pix-keys.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['walletId', 'keyType', 'keyValue'])

        for i in range(1, NUM_RECORDS + 1):
            key_type = key_types[(i - 1) % 4]

            if key_type == 'CPF':
                # Gera CPF numérico (11 dígitos)
                cpf = f'{i:011d}'
                key_value = cpf
            elif key_type == 'EMAIL':
                key_value = f'pix{i}@example.com'
            elif key_type == 'PHONE':
                phone = f'1198765{i:04d}'
                key_value = phone
            else:  # EVP
                key_value = str(uuid.uuid4())

            writer.writerow([i, key_type, key_value])
    print(f"✓ pix-keys.csv: {NUM_RECORDS} registros")

def generate_transfers():
    """Gera 200 transferências com chaves de idempotência únicas"""
    pix_keys = [
        'pix1@example.com', '11987650001', '00000000001',
        'pix2@example.com', '11987650002', '00000000002',
    ]

    with open(f'{BASE_DIR}/transfers.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['fromWalletId', 'pixKey', 'amount', 'idempotencyKey'])

        for i in range(1, NUM_RECORDS + 1):
            # Usa chave pix de outro usuário (rotação)
            pix_key_options = [
                f'pix{((i % 50) + 1)}@example.com',
                f'1198765{((i % 50) + 1):04d}',
                f'{((i % 50) + 1):011d}'
            ]
            pix_key = random.choice(pix_key_options)

            amount = round(random.uniform(75.0, 300.0), 2)
            idempotency_key = f'idempotency-key-{i:03d}'

            writer.writerow([i, pix_key, f'{amount:.2f}', idempotency_key])
    print(f"✓ transfers.csv: {NUM_RECORDS} registros")

def generate_transfers_expanded():
    """Gera transferências com chaves de idempotência únicas (expandido)"""
    with open(f'{BASE_DIR}/transfers.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['fromWalletId', 'pixKey', 'amount', 'idempotencyKey'])

        for i in range(1, NUM_TRANSFERS + 1):
            base = (i % 1000) + 1
            pix_key_options = [
                f'pix{base}@example.com',
                f'1198{base:06d}',
                f'{base:011d}'
            ]
            pix_key = random.choice(pix_key_options)

            amount = round(random.uniform(75.0, 300.0), 2)
            idempotency_key = f'idem-{i:05d}-{uuid.uuid4()}'

            writer.writerow([i, pix_key, f'{amount:.2f}', idempotency_key])
    print(f"✅ transfers.csv: {NUM_TRANSFERS} registros")

def generate_webhooks():
    """Gera 200 eventos webhook (CONFIRMED/REJECTED)"""
    base_time = datetime(2025, 10, 16, 12, 0, 0)

    with open(f'{BASE_DIR}/webhooks.csv', 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['endToEndId', 'eventId', 'eventType', 'occurredAt'])

        for i in range(1, NUM_RECORDS + 1):
            end_to_end_id = f'E{i:020d}'
            event_id = f'evt-{i:03d}'

            # 70% CONFIRMED, 30% REJECTED
            event_type = 'CONFIRMED' if i % 10 <= 6 else 'REJECTED'

            occurred_at = base_time + timedelta(seconds=i)
            occurred_at_str = occurred_at.strftime('%Y-%m-%dT%H:%M:%SZ')

            writer.writerow([end_to_end_id, event_id, event_type, occurred_at_str])
    print(f"✓ webhooks.csv: {NUM_RECORDS} registros")

if __name__ == '__main__':
    print(f"Gerando massas de dados ({NUM_RECORDS} registros cada)...")
    print()

    generate_wallets()
    generate_deposits()
    generate_withdrawals()
    generate_pix_keys()
    # usar massa expandida para transferências
    generate_transfers_expanded()
    generate_webhooks()

    print()
    print(f"✓ Todas as massas de dados foram geradas com sucesso!")
    print(f"  Total: {NUM_RECORDS * 6} registros")
