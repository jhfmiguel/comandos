# Evolução do Enterprise Core

## Objetivo

Construir capacidades empresariais comuns uma única vez em termos de modelo, contratos e regras, evitando três implementações divergentes.

## Fases

### Fase 1 — Contrato comum
Para cada capacidade:
- definir linguagem e entidades genéricas;
- separar regras universais das extensões verticais;
- documentar APIs públicas;
- criar testes de contrato;
- evitar nomes específicos de COMANDOS, TRATOR ou TUBARÃO.

### Fase 2 — Primeira implementação
A primeira implementação pode nascer dentro do produto que tiver a necessidade concreta mais madura, desde que respeite a fronteira Enterprise Core.

Ela deve ser escrita como código reutilizável, sem dependência da vertical.

### Fase 3 — Segundo consumidor
Quando um segundo produto precisar da mesma capacidade:
- comparar requisitos reais;
- corrigir generalizações prematuras;
- confirmar quais regras são verdadeiramente comuns;
- extrair ou sincronizar o módulo de forma controlada.

### Fase 4 — Extração física
Quando existir consumo real por múltiplos produtos, avaliar extração para biblioteca/repositório compartilhado da família pessoal/comercial.

A extração física só deve ocorrer quando:
- contratos estiverem estáveis;
- testes cobrirem o comportamento;
- dependências forem neutras;
- versionamento e compatibilidade estiverem definidos;
- nenhum domínio vertical estiver vazando para o compartilhado.

## Regra principal

Nunca copiar e evoluir independentemente o mesmo módulo empresarial em três repositórios por longo período.

Se uma capacidade é realmente comum, ela deve convergir para uma única fonte compartilhada.
