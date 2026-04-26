# CI/CD Pipeline Documentation

Este projeto utiliza **GitHub Actions** para automação de testes, build e deploy.

## Workflows Configurados

### 1. **CI Pipeline** (`ci.yml`)
Pipeline de Integração Contínua executado a cada push e pull request.

**Triggers:**
- Push para `main` e `develop`
- Pull requests para `main` e `develop`

**Jobs:**

#### `build-and-test`
- Testa com múltiplas versões de Java (11 e 17)
- Executa testes unitários com Maven
- Faz build do projeto
- Executa testes de integração (se configurados)
- Faz upload dos resultados de testes

**Artifacts gerados:**
- Relatórios de testes (surefire-reports)
- Logs de build (se houver falha)

#### `code-quality`
- Valida a estrutura do projeto
- Executa análise de estilo de código (checkstyle, se configurado)

#### `docker-build`
- Constrói a imagem Docker
- Utiliza cache para otimizar builds

---

### 2. **Docker Build and Publish** (`docker.yml`)
Publica imagem Docker no GitHub Container Registry.

**Triggers:**
- Push para `main` (com mudanças em src/, Dockerfile ou pom.xml)
- Disparo manual (workflow_dispatch)

**Features:**
- Autenticação automática com GHCR
- Tags semânticas (branch, version, SHA)
- Cache otimizado com GitHub Actions
- Publicação automática em release

**Para usar:**
```bash
docker pull ghcr.io/eltonmesquita87/pix_service_prova_tecnica:main
```

---

### 3. **Performance Tests** (`performance.yml`)
Executa testes de performance com Gatling.

**Triggers:**
- Push para `main` e `develop`
- Agenda semanal (domingo 2 AM UTC)
- Disparo manual

**Infraestrutura:**
- PostgreSQL 15 Alpine (containerizado)
- Aplicação iniciada automaticamente
- Testes Gatling contra a aplicação

**Resultados:**
- Relatórios de performance
- Simulações e métricas
- Armazenados por 90 dias

---

## Status dos Workflows

- ✅ **CI Pipeline** - Build e testes de cada PR/push
- ✅ **Docker Build** - Publicação automática
- ✅ **Performance** - Testes semanais e sob demanda

## Como Visualizar

1. Vá para **Actions** no repositório GitHub
2. Selecione o workflow desejado
3. Veja os status de execução e logs

## Configuração Recomendada (Branch Protection)

Para garantir qualidade, configure proteção de branch:

1. Vá para **Settings** > **Branches**
2. Adicione regra para `main` e `develop`:
   - ✅ Require status checks to pass
   - ✅ Require PR reviews (1+)
   - ✅ Dismiss stale PR approvals

## Variáveis de Ambiente

Para adicionar secrets ou variables, vá para **Settings** > **Secrets and variables** > **Actions**

Secrets úteis (opcional):
- `DOCKER_REGISTRY_URL` - Registry customizado
- `SONAR_TOKEN` - Para análise SonarQube

## Customizações Futuras

Você pode adicionar:
- [ ] SonarQube para análise de qualidade
- [ ] SAST (Static Application Security Testing)
- [ ] Verificação de dependências vulneráveis
- [ ] Deploy automático em staging/prod
- [ ] Notificações em Slack/Discord
- [ ] Relatórios de cobertura de testes
