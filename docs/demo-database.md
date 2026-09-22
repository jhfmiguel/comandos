# Banco de demonstração

O perfil `demo` é **destrutivo** e existe apenas para desenvolvimento.

Ao iniciar a API com esse perfil, o Hibernate usa `ddl-auto=create`: as tabelas
gerenciadas são recriadas e uma massa de dados de demonstração é inserida.

## Subir o banco de infraestrutura

```powershell
docker compose -f infrastructure\docker\compose.yml up -d postgres
```

## Recriar o banco e carregar os exemplos

```powershell
cd api
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Os exemplos incluem:
- organização pública e fornecedor;
- três unidades organizacionais;
- servidor, armeiro e fornecedor;
- perfis funcionais;
- cofres/almoxarifados;
- categorias de arma, munição e proteção balística;
- Beretta APX, munição 9 mm e colete III-A;
- cinco bens serializados;
- lote com 5.000 munições e saldo correspondente.

Para voltar ao modo normal, inicie a aplicação sem o perfil `demo`.
