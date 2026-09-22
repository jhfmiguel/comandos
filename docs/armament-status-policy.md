# Política canônica de status de ativo

Status válidos de um ativo serializado no COMANDOS:

- AVAILABLE — disponível para operação;
- CUSTODIED — cautelado;
- IN_MAINTENANCE — em manutenção;
- TRANSFER_PENDING — enviado e aguardando aceite da unidade destino;
- BLOCKED — bloqueado para uso;
- MISSING — desaparecido/extraviado;
- RESTRICTED — restrição administrativa/operacional;
- SOLD — alienado/vendido;
- DISPOSED — baixado/destruído.

SOLD e DISPOSED são terminais para os fluxos ordinários.

As operações devem consultar EquipmentStatePolicy antes de alterar um ativo. O
código canônico vive em AssetStatus; novas variações nominais não devem ser
introduzidas em serviços ou telas.
