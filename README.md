# ProspectAI

Aplicativo Android para localizar estabelecimentos próximos e organizar possíveis oportunidades comerciais de presença digital.

## Funcionalidades implementadas
- Android nativo com Kotlin e Jetpack Compose.
- Interface escura.
- Solicitação de localização durante o uso do aplicativo.
- Pesquisa de estabelecimentos em raio de 30 km.
- Consulta ao OpenStreetMap por meio da Overpass API.
- Exibição de nome, categoria, telefone e endereço quando esses dados estiverem cadastrados.
- Separação entre estabelecimentos com endereço de site cadastrado e possíveis oportunidades sinalizadas por um ponto vermelho.

## Atenção sobre os resultados
O ponto vermelho indica que não há endereço de site cadastrado para aquele estabelecimento nos dados do OpenStreetMap. Isso **não prova** que a empresa não tenha um site; cada oportunidade deve ser conferida manualmente antes do contato.

A cobertura depende dos dados mapeados pela comunidade. A Overpass API é um serviço público compartilhado, sujeito a limites, lentidão e indisponibilidade. Uma busca de 30 km pode levar algum tempo ou falhar temporariamente em regiões com muitos dados.

## Fonte e atribuição
Os dados geográficos vêm do OpenStreetMap. © OpenStreetMap contributors. Consulte https://www.openstreetmap.org/copyright.

## Privacidade
A localização é solicitada quando o usuário inicia uma pesquisa. Esta versão não implementa rastreamento em segundo plano nem envia a localização para um servidor próprio; as coordenadas são usadas para consultar a Overpass API.

## Executar
Abra a pasta raiz no Android Studio, sincronize o Gradle e execute o módulo `app` em um dispositivo ou emulador Android. É necessário acesso à internet e serviços de localização do Google Play no dispositivo.
