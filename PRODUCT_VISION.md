# Afilaxy — Visão de Produto e Problema Central

> Este documento define o problema que o Afilaxy resolve e deve ser consultado antes de qualquer decisão de feature, UX ou conteúdo no app e no site.

---

## O Problema

O Afilaxy resolve a **falha crítica de um sistema que "perde" o asmático entre consultas**, gerando uma gestão reativa da doença.

O foco é a **baixa adesão ao tratamento** e o **isolamento clínico**, manifestados na **"automedicação social"** — o ato de pedir/emprestar inaladores em emergências.

Esse hábito revela que muitos brasileiros com asma gerenciam a condição sem dados nem vínculo com especialistas e só chegam ao sistema de saúde nas crises graves.

---

## Relevância na Saúde Pública

> **Regra:** os números epidemiológicos do Afilaxy (app, site, pitch, editais) vêm **somente** da revisão
> *Internações por asma no Brasil segundo o DATASUS* (Costa et al., Lumen et Virtus, 2026 —
> DOI 10.56238/levv17n57-045) e dos estudos que ela cita. Não usar números sem essa fonte.
> Não usar as "350 mil internações anuais" da introdução da revisão: os estudos citados nela
> indicam cerca de 71 a 80 mil por ano.

- **Pessoas com asma:** cerca de **20 milhões de brasileiros** (Ministério da Saúde, 2025, citado na revisão).
- **Internações no SUS:** **403.135** entre 2016 e 2020 (≈ 80 mil/ano; Marques et al., 2022) e **354.201** entre 2019 e 2023 (≈ 71 mil/ano; Mouhana et al., 2025). O Nordeste lidera as internações.
- **Óbitos registrados:** **2.248** entre 2016 e 2020 (≈ 450/ano), concentrados no Sudeste (893) e no Nordeste (746) (Marques et al., 2022).
- **Custo das internações para o SUS:** **R$ 231,9 milhões** entre 2016 e 2020 (≈ R$ 46 milhões/ano), com o Nordeste à frente (R$ 84,2 milhões) (Marques et al., 2022).
- **Quem é mais afetado:** crianças de 1 a 4 anos são a faixa com mais internações. Idosos concentram os óbitos (46,4% entre 75 e 89 anos; Pinto et al., 2023), com predomínio feminino (63,5% dos óbitos entre 2000 e 2015; Cavalcante; Oliveira, 2020).
- O Afilaxy busca reduzir crises e internações por meio de **monitoramento remoto e prevenção**.

---

## Como o Afilaxy mede o descontrole da asma

**Métrica principal: frequência de uso do medicamento de resgate (alívio).** O app combina dois sinais:

1. **Autorrelato de uso da bombinha de resgate** no check-in noturno (`rescueInhalerUses`), comparado com o critério da GINA: usar o alívio **mais de 2 vezes por semana** indica asma não controlada. É também uma das perguntas do ACT.
2. **Pedido de bombinha emprestada** (emergência P2P): mostra que a pessoa precisou do resgate e não tinha o próprio à mão.

**O que essa métrica mede:** o **controle** da asma, ou seja, se o paciente está fora do protocolo. Isso está de acordo com as boas práticas (GINA, ACT), e o uso frequente do resgate também prevê crises graves.

**O que ela não mede diretamente:** a **adesão** ao remédio de controle. Usar muito o resgate é um **sinal indireto** de possível baixa adesão, mas pode ter outras causas: técnica inalatória errada, tratamento insuficiente para a gravidade, gatilhos ambientais ou comorbidades. Por isso, no app e nos materiais, chamamos essa métrica de **indicador de descontrole** e de **sinal de alerta para baixa adesão**, nunca de "medida de adesão".

**Cuidado:** quando o alívio prescrito é budesonida + formoterol (estratégia preferida pela GINA), cada uso do "resgate" também leva corticoide. Nesse caso, o número de usos deve ser lido com mais cautela.

---

## Proposta de Valor

Sendo a **única plataforma digital de gestão de asma em português no país**, o Afilaxy:

- Transforma dados do paciente em **Real-World Evidence (RWE)**
- Antecipa crises antes que aconteçam
- Otimiza a gestão populacional da doença

---

## Implicações para Desenvolvimento

Toda feature, tela ou conteúdo deve ser avaliado contra esta pergunta:

> **"Isso ajuda o paciente a sair do isolamento clínico, aderir ao tratamento ou prevenir uma crise — em vez de apenas reagir a uma?"**

### ✅ Features alinhadas com a visão
- Monitoramento contínuo de sintomas e uso de medicação
- Alertas preditivos baseados em dados ambientais e histórico
- Geração de relatórios para médicos e pacientes
- Registro de check-ins proativos (manhã/noite)
- Localização de apoio em emergências (automedicação social)

### ⚠️ Features a questionar
- Qualquer funcionalidade que só atue *durante* a crise, sem contribuir para a prevenção
- Conteúdo que posicione o app como utilitário de emergência (e não plataforma de gestão)

---

## Posicionamento Público (loja / site)

O Afilaxy deve ser apresentado como **plataforma de gestão contínua de asma**, não como app de emergência. A "automedicação social" é o ponto de entrada narrativo — o sintoma visível de um problema sistêmico que o Afilaxy resolve na raiz.
