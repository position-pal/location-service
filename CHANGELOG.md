## [2.0.2](https://github.com/position-pal/location-service/compare/2.0.1...2.0.2) (2025-01-09)

### Dependency updates

* **deps:** update akka to v2.10.0 (minor) ([#70](https://github.com/position-pal/location-service/issues/70)) ([85472d9](https://github.com/position-pal/location-service/commit/85472d935485ce4a4755f9b9af2dca651acdef66))
* **deps:** update dependency ch.qos.logback:logback-classic to v1.5.16 ([#148](https://github.com/position-pal/location-service/issues/148)) ([addcc46](https://github.com/position-pal/location-service/commit/addcc464138a2736fbb07402440daf12c0fa8b8f))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.118 ([#150](https://github.com/position-pal/location-service/issues/150)) ([06865ca](https://github.com/position-pal/location-service/commit/06865ca54468b3e2ab9400aad2dd0bf31fdfe7b6))
* **deps:** update node.js to 22.13 ([#149](https://github.com/position-pal/location-service/issues/149)) ([f440d69](https://github.com/position-pal/location-service/commit/f440d69d4e4f52c8037516023ff327f401aef7fc))
* **deps:** update plugin scala-extras to v3.1.3 ([c37cf90](https://github.com/position-pal/location-service/commit/c37cf90529276d528d9905b9dc1e9a5ad6be2c62))
* **deps:** update plugin scala-extras to v3.1.4 ([ec92f84](https://github.com/position-pal/location-service/commit/ec92f8455b6b5c6c660c1f0651f945e0dfcfa82a))

### Bug Fixes

* **application:** do not trigger multiple notifications for the same alert in check reactions ([#146](https://github.com/position-pal/location-service/issues/146)) ([643efdc](https://github.com/position-pal/location-service/commit/643efdc579fddfe68f21a3ef64990ba46a5ab4a1))

### Tests

* add architectural unit test ([9ce060f](https://github.com/position-pal/location-service/commit/9ce060fa09413006fc4039de0213fc520654ce8d))

### Build and continuous integration

* **deps:** pin dependencies ([#144](https://github.com/position-pal/location-service/issues/144)) ([566e54b](https://github.com/position-pal/location-service/commit/566e54b04a5a79a3019cd38299f5d349f5491f50))
* **deps:** update docker/build-push-action digest to b32b51a ([#147](https://github.com/position-pal/location-service/issues/147)) ([763404f](https://github.com/position-pal/location-service/commit/763404f1e68497b2687a9eab4f97308cf6431caf))

### Style improvements

* format accordingly to qa rules ([576fd1a](https://github.com/position-pal/location-service/commit/576fd1a80d4a943fdacffd4bb695e7ead4e22a37))
* improve readability ([acb816f](https://github.com/position-pal/location-service/commit/acb816f18ca4358d64b6e98d723b821b175994b9))

## [2.0.1](https://github.com/position-pal/location-service/compare/2.0.0...2.0.1) (2025-01-02)

### Bug Fixes

* **ws:** use as overflow strategy `dropHead` and make sure to unwire all observers for a leaving user ([f762e99](https://github.com/position-pal/location-service/commit/f762e99caf7fe6e178150c77a699ea2f77c34825))

### Build and continuous integration

* **scaladoc:** checkout repository to tag of the release version ([38f3638](https://github.com/position-pal/location-service/commit/38f3638e54f552815e9ac5fb9460e1114f67f882))
* upload fatjar during build job to download it and use during image building and add dry-delivery ([cabc665](https://github.com/position-pal/location-service/commit/cabc665324f31698ccf80028f4da5efc97840d14))

### Refactoring

* changing configuration of the service in order to operate with kubernetes ([acb5096](https://github.com/position-pal/location-service/commit/acb5096961338b2c10bdc9dc3091d03a5ced0d39))

## [2.0.0](https://github.com/position-pal/location-service/compare/1.0.0...2.0.0) (2024-12-26)

### ⚠ BREAKING CHANGES

* **ws:** switch to versioned ws, starting from v1

### Dependency updates

* **deps:** update plugin org.danilopianini.gradle-pre-commit-git-hooks to v2.0.18 ([#140](https://github.com/position-pal/location-service/issues/140)) ([67d4440](https://github.com/position-pal/location-service/commit/67d4440beb3999502baa83a5edd161b65d3d07cd))

### Build and continuous integration

* add issues and pr permissions and use position-pal bot token to release ([953ba02](https://github.com/position-pal/location-service/commit/953ba02a8feb9670157461a475a7ade9ea9b1a44))
* fetch tags when checkout repository in doc generation ([2899163](https://github.com/position-pal/location-service/commit/2899163c1229ed974e2c4d1686351f13a0cb2384))

### General maintenance

* update readme with link to scaladoc ([a8b9c46](https://github.com/position-pal/location-service/commit/a8b9c46e45047d33304f9041fda6d33313dc9c76))

### Refactoring

* **ws:** log income and outgoing messages ([7841ccf](https://github.com/position-pal/location-service/commit/7841ccfcdc661c047f6087b66909947204d13b76))
* **ws:** switch to versioned ws, starting from v1 ([fdc0389](https://github.com/position-pal/location-service/commit/fdc0389ccf968d335d1f3c16cb4f0574cc6ffeb3))

## [1.0.0](https://github.com/position-pal/location-service/compare/0.7.1...1.0.0) (2024-12-23)

### ⚠ BREAKING CHANGES

* **entrypoint:** add complete entrypoint
* **tracking:** add group info to domain events to make the projection work scope-based

### Features

* **application:** add basic user groups service impl ([8b77e0e](https://github.com/position-pal/location-service/commit/8b77e0ee61ea4e850e686db56e93bb8055a2c9a5))
* **entrypoint:** add complete entrypoint ([75f56db](https://github.com/position-pal/location-service/commit/75f56dbdbc1c6c26abc4036afab5b41e835760dd))
* **entrypoint:** add entrypoint skeleton ([9c54914](https://github.com/position-pal/location-service/commit/9c5491468df9912b4ed08ff16783fd6cfd753197))

### Dependency updates

* **deps:** update dependency ch.qos.logback:logback-classic to v1.5.13 ([#129](https://github.com/position-pal/location-service/issues/129)) ([22613d0](https://github.com/position-pal/location-service/commit/22613d0acf0b0f38dbec4463006bc03c971c8f61))
* **deps:** update dependency ch.qos.logback:logback-classic to v1.5.14 ([#133](https://github.com/position-pal/location-service/issues/133)) ([a560ae4](https://github.com/position-pal/location-service/commit/a560ae4381a81bdb256aa51564e28f17f413db18))
* **deps:** update dependency ch.qos.logback:logback-classic to v1.5.15 ([#138](https://github.com/position-pal/location-service/issues/138)) ([0797a05](https://github.com/position-pal/location-service/commit/0797a05af22fb322123003d1319165faa0bdec9f))
* **deps:** update dependency com.google.protobuf:protoc to v4.29.2 ([#130](https://github.com/position-pal/location-service/issues/130)) ([f8f6894](https://github.com/position-pal/location-service/commit/f8f6894dcbf1f0f57901fce5bf90ca3f17c69119))
* **deps:** update dependency dev.hnaderi:named-codec-circe_3 to v0.3.0 ([#132](https://github.com/position-pal/location-service/issues/132)) ([155d5c6](https://github.com/position-pal/location-service/commit/155d5c6579107918f48f15a1bd52e37a38fd8336))
* **deps:** update dependency gradle to v8.12 ([#135](https://github.com/position-pal/location-service/issues/135)) ([412ce37](https://github.com/position-pal/location-service/commit/412ce3768dc97b144114f6267fe2e63a91b45c6e))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.117 ([#139](https://github.com/position-pal/location-service/issues/139)) ([550327f](https://github.com/position-pal/location-service/commit/550327fe8e6ec9ced4d7fddb2b14a4761034f72d))
* **deps:** update eclipse-temurin:21 docker digest to 843686b ([#114](https://github.com/position-pal/location-service/issues/114)) ([f0dd1e6](https://github.com/position-pal/location-service/commit/f0dd1e62338443ebb1679279a9b52b9d5e38d435))
* **deps:** update junit5 monorepo to v1.11.4 ([#123](https://github.com/position-pal/location-service/issues/123)) ([6a01c24](https://github.com/position-pal/location-service/commit/6a01c245c655555519cea23329e99393f8e0bd60))
* **deps:** update lepus to v0.5.4 ([#124](https://github.com/position-pal/location-service/issues/124)) ([c1aabe9](https://github.com/position-pal/location-service/commit/c1aabe9cd043d9d26d17afc367e2e9b7cb428a26))
* **deps:** update plugin com.gradle.develocity to v3.19 ([#118](https://github.com/position-pal/location-service/issues/118)) ([b186f00](https://github.com/position-pal/location-service/commit/b186f006c5fcaa708d3c608e008dc3d709e6fc4b))
* **deps:** update plugin gradle-docker-compose to v0.17.12 ([#120](https://github.com/position-pal/location-service/issues/120)) ([f4133cb](https://github.com/position-pal/location-service/commit/f4133cbab8164c3c91f119a2a8d789429789fe74))
* **deps:** update plugin org.danilopianini.gradle-pre-commit-git-hooks to v2.0.17 ([#125](https://github.com/position-pal/location-service/issues/125)) ([50cc944](https://github.com/position-pal/location-service/commit/50cc944d0585f0ee84e02f9ef40b0d1a58eb949b))
* **deps:** update plugin scala-extras to v3 ([#111](https://github.com/position-pal/location-service/issues/111)) ([1e84615](https://github.com/position-pal/location-service/commit/1e84615f4ff56f63286bf17c36011f9aca44c95a))
* **deps:** update rabbitmq:4-management docker digest to 144d782 ([#116](https://github.com/position-pal/location-service/issues/116)) ([15a820a](https://github.com/position-pal/location-service/commit/15a820a50a2a73e95abfff064a9dde2bea56a486))
* **deps:** update rabbitmq:4-management docker digest to 4a2b95d ([#131](https://github.com/position-pal/location-service/issues/131)) ([71598cf](https://github.com/position-pal/location-service/commit/71598cf0ddac6cfb569e611996122e191659c158))
* **deps:** update rabbitmq:4-management docker digest to 57513d2 ([#115](https://github.com/position-pal/location-service/issues/115)) ([3a8e07f](https://github.com/position-pal/location-service/commit/3a8e07f59090d382c906351902ef13d1b6ceff4f))

### Bug Fixes

* **reactions:** from routing to SOS in went offline logic ([1e8a10c](https://github.com/position-pal/location-service/commit/1e8a10cb20d623bd3b633e89ff23f09bcdb2585b))
* **tracking:** encode scope in expected format ([40afe89](https://github.com/position-pal/location-service/commit/40afe89a570981232be7945686c0b729be4370bd))
* **tracking:** start a timer to realize an user went offline ([3bfdf11](https://github.com/position-pal/location-service/commit/3bfdf11bb7ff9bee7b319cb7ae88de33c96b8580))

### Tests

* add cucumbers features and location sharing steps ([46bab12](https://github.com/position-pal/location-service/commit/46bab12ce4f0e6846d8fbccd3f160fe5e077646e))
* **application:** adapt tests to use kernel `NotificationMessage` in place of Strings ([79b0ae1](https://github.com/position-pal/location-service/commit/79b0ae1abe612b863e1d286a7b5047a0faef9c4d))
* remove unuseful print and improve testable akka config ([a9821e9](https://github.com/position-pal/location-service/commit/a9821e9ecee22551d2450b92c835e5fe1461ac39))
* use test fixtures plugin to dry test utilities ([98a93b5](https://github.com/position-pal/location-service/commit/98a93b59d98a595c16953214970b93e2ff0b3603))

### Build and continuous integration

* add git semantic versioning and shadow jar plugins ([62c375e](https://github.com/position-pal/location-service/commit/62c375eae43a9b9e43badb125a1d7fafd1cc6cd1))
* add template sync workflow ([6196efa](https://github.com/position-pal/location-service/commit/6196efabac51f047b553bd26b4e8b3a480b654a2))
* **deps:** update actions/setup-java digest to 7a6d8a8 ([#127](https://github.com/position-pal/location-service/issues/127)) ([823624a](https://github.com/position-pal/location-service/commit/823624adad5e42fe7e88f6f34849700a893840eb))
* **deps:** update docker/setup-buildx-action digest to 6524bf6 ([#121](https://github.com/position-pal/location-service/issues/121)) ([3e7be3e](https://github.com/position-pal/location-service/commit/3e7be3eca3e99fda2228cdcbbe2239205b41845b))
* drop template sync action, aggregate delivery-related jobs in a single workflow file and scaladoc generation ([003bd91](https://github.com/position-pal/location-service/commit/003bd9196c02d1830f16f83017a96ac03d314557))
* drop unused plugins ([270566b](https://github.com/position-pal/location-service/commit/270566b99e92533d8dd1c329be7a6ee51a5dba4c))
* fix `is_force_push_pr` parameter position and add `pr_title` ([1ad6ccc](https://github.com/position-pal/location-service/commit/1ad6ccc16f7046cd0057b7b9df2748b0980b1e8b))
* fix cron job to run daily at 00:00 instead of at 00:00 on day-of-month 1 ([79a65e7](https://github.com/position-pal/location-service/commit/79a65e75a7445c8f1026a82a40b0041807f3e1c7))
* use custom pat in order to push workflow updates ([525d489](https://github.com/position-pal/location-service/commit/525d48930ad04b8338f31d10b2fe02964ac58de3))

### General maintenance

* add prerequisites to readme ([68c9228](https://github.com/position-pal/location-service/commit/68c9228a3c7ae41d46819e27976f751248e7d7a7))

### Refactoring

* **application:** drop `Async` bound to effect type ([2326000](https://github.com/position-pal/location-service/commit/2326000d8ddc7c5fa6fbbdd74a4d0213e1730224))
* **application:** separate reactions in a file each and add `EventPreCheckNotifier` ([63271e7](https://github.com/position-pal/location-service/commit/63271e780d91d177f228d9c4a2e878009cce3932))
* **application:** use kernel `NotificationMessage` and move tracking related services in a per se package ([059b832](https://github.com/position-pal/location-service/commit/059b832ffb69cb4e299a4d7106b25a31d5c4c484))
* **build:** use `==` instead of `equals` ([b3c0fa6](https://github.com/position-pal/location-service/commit/b3c0fa67b43999d789c60b08385867ec1f051dcc))
* **domain:** make session scoped to a specific user and group ([405f764](https://github.com/position-pal/location-service/commit/405f7642990caa141624ba2073737aeff0ba95a3))
* **domain:** move in domain the event reaction adt and add time utils ([ec940c1](https://github.com/position-pal/location-service/commit/ec940c14fdb811bb3942451a70458cbdb67cce14))
* **entrypoint:** improve readability of entrypoint ([94e5c12](https://github.com/position-pal/location-service/commit/94e5c1294b73a265debb7256e7719a1ae1580a30))
* **grpc:** modify grpc endpoint requests to get a scope instead of only the user ([d2a2b9d](https://github.com/position-pal/location-service/commit/d2a2b9d3305e2795b38ca75d66105fd98508b690))
* **presentation:** adapt seriaizers to use scoped sessions ([1703fbb](https://github.com/position-pal/location-service/commit/1703fbbba09e33aff1e7fa598bd057b98fc740d1))
* **storage:** save scoped sessions ([62b9ff8](https://github.com/position-pal/location-service/commit/62b9ff828c463d91301ada94c5378197bd3e6077))
* **tracking:** add group info to domain events to make the projection work scope-based ([666c1fb](https://github.com/position-pal/location-service/commit/666c1fb7d2fafbeafab34ee1a7b842d5cf23e12c))
* **tracking:** rename infrastructure in tracking module ([7161368](https://github.com/position-pal/location-service/commit/7161368475bb89b5580b6ccea6dae917124017be))
* **ws:** move websockets in a separate submodule ([4345f70](https://github.com/position-pal/location-service/commit/4345f70405d12233cf815d5e03396f575c112fb7))
* **ws:** use scoped sessions ([2b36309](https://github.com/position-pal/location-service/commit/2b36309855d54962a446c052835458772f82e516))

## [0.7.1](https://github.com/position-pal/location-service/compare/0.7.0...0.7.1) (2024-12-12)

### Dependency updates

* **core-deps:** update dependency org.scala-lang:scala3-library_3 to v3.6.2 ([#112](https://github.com/position-pal/location-service/issues/112)) ([4756fe4](https://github.com/position-pal/location-service/commit/4756fe4890cad1688f41020d9c56502fd75add96))
* **deps:** pin rabbitmq docker tag to 5c3ead8 ([#100](https://github.com/position-pal/location-service/issues/100)) ([938fcd3](https://github.com/position-pal/location-service/commit/938fcd3cc19d5740ba754b15956f7a5f49ab4da9))
* **deps:** update borer to v1.15.0 ([#108](https://github.com/position-pal/location-service/issues/108)) ([7a8074e](https://github.com/position-pal/location-service/commit/7a8074e26bb8c0ce8d4b15ac6fc014fa78319a42))
* **deps:** update dependency com.google.protobuf:protoc to v4.29.0 ([#102](https://github.com/position-pal/location-service/issues/102)) ([7684c10](https://github.com/position-pal/location-service/commit/7684c10207b452450b9740e06b9f398496ddf616))
* **deps:** update dependency com.google.protobuf:protoc to v4.29.1 ([#106](https://github.com/position-pal/location-service/issues/106)) ([c0e18ea](https://github.com/position-pal/location-service/commit/c0e18eaa9c2c30bfb966c2fd8d454ae535755e78))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.116 ([#110](https://github.com/position-pal/location-service/issues/110)) ([8bb109a](https://github.com/position-pal/location-service/commit/8bb109a5a47b0a96cb7ba106bfca9c788d8318e6))
* **deps:** update eclipse-temurin:21 docker digest to 30cda46 ([#104](https://github.com/position-pal/location-service/issues/104)) ([6c336ff](https://github.com/position-pal/location-service/commit/6c336ff1197e611796f9fc5113433aaee068f633))
* **deps:** update grpc-java monorepo to v1.68.2 ([#101](https://github.com/position-pal/location-service/issues/101)) ([de87900](https://github.com/position-pal/location-service/commit/de879009f649e26b666e6e0c8d233c054c5ba20d))
* **deps:** update grpc-java monorepo to v1.69.0 ([#113](https://github.com/position-pal/location-service/issues/113)) ([f222234](https://github.com/position-pal/location-service/commit/f222234f4f0613ac6fc4b0270a89481bf054c818))
* **deps:** update http4s to v1.0.0-m44 ([#109](https://github.com/position-pal/location-service/issues/109)) ([c3250ef](https://github.com/position-pal/location-service/commit/c3250ef4fb795294becd121b0961792145ad7878))
* **deps:** update node.js to 22.12 ([#105](https://github.com/position-pal/location-service/issues/105)) ([6d1743c](https://github.com/position-pal/location-service/commit/6d1743c9f14f4c893bff56efd8d71644647cd066))
* **deps:** update rabbitmq docker tag to v4 ([#103](https://github.com/position-pal/location-service/issues/103)) ([c9e86f7](https://github.com/position-pal/location-service/commit/c9e86f7e3ac0f050dae5469f20640142fdaba80a))
* **deps:** update rabbitmq:4-management docker digest to 8340b65 ([#107](https://github.com/position-pal/location-service/issues/107)) ([2e182de](https://github.com/position-pal/location-service/commit/2e182de5798dee3452e9ef79bb374cf3ad5c5db6))

## [0.7.0](https://github.com/position-pal/location-service/compare/0.6.1...0.7.0) (2024-12-02)

### Features

* **application:** add FilterableReaction mixin ([3ae41d0](https://github.com/position-pal/location-service/commit/3ae41d01e7393bc9325194011f5fb0e134b9fcfa))
* **application:** add user sessions service ([4a714fd](https://github.com/position-pal/location-service/commit/4a714fd21adf7af248ed085e8dec5335b487cfaf))
* **grpc:** add grpc service sessions implementation ([6f0b175](https://github.com/position-pal/location-service/commit/6f0b175b6854c4ea7a3aa85693de7bebe1fdc720))
* **messages:** add RabbitMQ groups related events consumer ([a006098](https://github.com/position-pal/location-service/commit/a0060988315c9793fee2b89f94d600e0394f6aa9))
* **messages:** add RabbitMQ notification publisher ([e219770](https://github.com/position-pal/location-service/commit/e21977092ae6b0aa268946cdebbd0b7aaf610f27))
* **presentation:** add grpc services definitions ([cf976e3](https://github.com/position-pal/location-service/commit/cf976e34192331a31f559407aae9470e9fb4490b))
* **storage:** add postgres groups repository adapter ([f138fca](https://github.com/position-pal/location-service/commit/f138fcad121c9af6daac00c9175fcd151801e188))

### Dependency updates

* **deps:** update dependency gradle to v8.11.1 ([#94](https://github.com/position-pal/location-service/issues/94)) ([0860827](https://github.com/position-pal/location-service/commit/0860827cc2c6cd889ef5abf6adf6cb2e762b9547))
* **deps:** update plugin gradle-docker-compose to v0.17.11 ([#93](https://github.com/position-pal/location-service/issues/93)) ([1701990](https://github.com/position-pal/location-service/commit/170199025d8451eec31f44f3995af210bb7c2f71))
* **deps:** update plugin org.danilopianini.gradle-pre-commit-git-hooks to v2.0.14 ([#92](https://github.com/position-pal/location-service/issues/92)) ([7a205e2](https://github.com/position-pal/location-service/commit/7a205e243333743ac8d73569c671c6653d4b23bb))
* **deps:** update plugin org.danilopianini.gradle-pre-commit-git-hooks to v2.0.15 ([#96](https://github.com/position-pal/location-service/issues/96)) ([bcd9ea1](https://github.com/position-pal/location-service/commit/bcd9ea1c305ad6f2da96cc61a307a19b43549ac5))
* **deps:** update pureconfig to v0.17.8 ([#95](https://github.com/position-pal/location-service/issues/95)) ([072da14](https://github.com/position-pal/location-service/commit/072da14ef692f22db72a9af417d82daf2372e2db))

### Bug Fixes

* **build:** inject env variables only after having evaluated subprojects ([58240fa](https://github.com/position-pal/location-service/commit/58240fa510adb500c332c7018fc35e49e2d7828c))

### Documentation

* add missing scala docs ([8cf8409](https://github.com/position-pal/location-service/commit/8cf84094a33fbc88a650ecadc3b04ace0b6a310c))

### Tests

* **grpc:** add test for session api and grpc configuration ([f61c590](https://github.com/position-pal/location-service/commit/f61c59049b99e29da4a20429ffac93c4d40c4653))
* **storage:** drop unnecessary akka storage configuration ([6300bff](https://github.com/position-pal/location-service/commit/6300bffad3a98db25a50bf704c091e9fc503c35f))
* use exhaustive matching ([afc1135](https://github.com/position-pal/location-service/commit/afc1135652cb81e6d973ade7bb5482cfa8de7dec))

### Build and continuous integration

* add env token for getting shared kernel ([3f28069](https://github.com/position-pal/location-service/commit/3f280692177b996e790b80ce372da186666829c0))
* **deps:** update docker/build-push-action digest to 48aba3b ([#97](https://github.com/position-pal/location-service/issues/97)) ([eb8b639](https://github.com/position-pal/location-service/commit/eb8b63958b096e36831edea22a01e5595354d62f))
* leverage build dsl to conditionally configure tests ([008dfc9](https://github.com/position-pal/location-service/commit/008dfc9fec79e81fbd88b73a385209beb6b3e09e))

### Style improvements

* apply qa rules ([75d6b89](https://github.com/position-pal/location-service/commit/75d6b893e72e7a8e3bfc1afbbdc677813670bbfa))
* **presentation:** use lowercase letter for methods ([e098a11](https://github.com/position-pal/location-service/commit/e098a111d8896fc1757aba8908d40cfa18b8f2c2))

### Refactoring

* **commons:** align object name with file name ([3b3bbc3](https://github.com/position-pal/location-service/commit/3b3bbc33beb97f2dfbbd7a55c1de6bf37245bfbf))
* **commons:** use common ConnectionFactory ([ccd0f05](https://github.com/position-pal/location-service/commit/ccd0f057ba5ca0a2a4e7a9f745745c2cfee16c6e))
* **messages:** add configuration validation ([c9adb6d](https://github.com/position-pal/location-service/commit/c9adb6d8c897451a930b264eb0b0b1d7ac2477c6))
* **messages:** dry extracting common behaviors ([ea9549e](https://github.com/position-pal/location-service/commit/ea9549e6e40d1861f3f1e9711f7e138b30a03be3))
* use UserId and GroupId from shared kernel ([2ede795](https://github.com/position-pal/location-service/commit/2ede795052191d4124329b80762dce2d8aa655c7))

## [0.6.1](https://github.com/position-pal/location-service/compare/0.6.0...0.6.1) (2024-11-20)

### Dependency updates

* **core-deps:** update dependency org.scala-lang:scala3-library_3 to v3.6.1 ([44a78de](https://github.com/position-pal/location-service/commit/44a78de726f7f0270dc9e171314d5a38b40731ee))
* **deps:** update eclipse-temurin:21 docker digest to b5fc642 ([#91](https://github.com/position-pal/location-service/issues/91)) ([51b6691](https://github.com/position-pal/location-service/commit/51b6691fe0d1d0abe746b94000a0e6972f28689b))
* **deps:** update plugin com.gradle.develocity to v3.18.2 ([#86](https://github.com/position-pal/location-service/issues/86)) ([020c3dd](https://github.com/position-pal/location-service/commit/020c3dda763b004a891bc6a5b862c7858f3f7919))

### Bug Fixes

* **application:** use Monad instance of Sync instead of overwrite explicitly ([b17afd5](https://github.com/position-pal/location-service/commit/b17afd55b59440ff7e9bb25901022179547a5a5a))

### Tests

* **infrastructure:** add configuration of futures completion patience and move in site call the eventually patience ([0e49f41](https://github.com/position-pal/location-service/commit/0e49f417ce538268edeac37576c5a22c8fe5c2f8))

### Build and continuous integration

* **deps:** pin dependencies ([#83](https://github.com/position-pal/location-service/issues/83)) ([1305149](https://github.com/position-pal/location-service/commit/1305149cc880da0cc6ab5c329cc6ac655de94ab5))
* **deps:** pin dependencies ([#90](https://github.com/position-pal/location-service/issues/90)) ([4f5c47c](https://github.com/position-pal/location-service/commit/4f5c47c9da826365872e226a815b1ee625ec93d5))
* do not try to publish docker images without a release of semantic release ([8147920](https://github.com/position-pal/location-service/commit/8147920fcc289eb29ebc3ab8539e1ccbe2f93321))

## [0.6.0](https://github.com/position-pal/location-service/compare/0.5.0...0.6.0) (2024-11-14)

### Features

* **ci:** add docker release workflow ([536c0e9](https://github.com/position-pal/location-service/commit/536c0e96b91c121848886473a45b5684f435ee34))
* **infrastructure:** add projection ([bf17079](https://github.com/position-pal/location-service/commit/bf1707921b27345dbe1694a43468a5288e9c8d7e))

### Dependency updates

* **deps:** update dependency com.lightbend.akka:akka-persistence-r2dbc_3 to v1.2.6 ([#68](https://github.com/position-pal/location-service/issues/68)) ([9f45a7b](https://github.com/position-pal/location-service/commit/9f45a7b4d4c2a12e942edcef8bb65cf336e07e5f))
* **deps:** update dependency com.typesafe.akka:akka-http_3 to v10.6.3 ([608a548](https://github.com/position-pal/location-service/commit/608a548045810d360641902858fe53c1b95b827a))
* **deps:** update dependency gradle to v8.11 ([#81](https://github.com/position-pal/location-service/issues/81)) ([378a2ea](https://github.com/position-pal/location-service/commit/378a2ea7588fe026f6b4612ecf5b1577744fc946))
* **deps:** update dependency org.scalatestplus:junit-5-10_3 to v3.2.19.1 ([#53](https://github.com/position-pal/location-service/issues/53)) ([6ef788c](https://github.com/position-pal/location-service/commit/6ef788c6c445b0aaf3344ab286d811d8a9a5fcc9))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.113 ([9ccbb91](https://github.com/position-pal/location-service/commit/9ccbb9179b9f3f01d4b6e903441978c2d094dcae))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.114 ([9562c4d](https://github.com/position-pal/location-service/commit/9562c4deacb3746c5d062fd70042a7a0ade00225))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.115 ([#82](https://github.com/position-pal/location-service/issues/82)) ([cd155e0](https://github.com/position-pal/location-service/commit/cd155e01dab90800164a80276631e35f3cd9786d))
* **deps:** update http4s to v1.0.0-m43 ([f0a4580](https://github.com/position-pal/location-service/commit/f0a45807361d7aaa583d8cf9acd6195449f5a23f))
* **deps:** update node.js to v22 ([73c2a87](https://github.com/position-pal/location-service/commit/73c2a87194fe7f7f0ae03a71cb4b19f5ed71ea46))
* **deps:** update postgres:17.0 docker digest to 2838b35 ([#78](https://github.com/position-pal/location-service/issues/78)) ([0b736aa](https://github.com/position-pal/location-service/commit/0b736aaa0ceaec82d25caeef6e43cb7d5358ae2f))
* **deps:** update postgres:17.0 docker digest to f176fef ([#80](https://github.com/position-pal/location-service/issues/80)) ([5955f97](https://github.com/position-pal/location-service/commit/5955f9749593619428d2c617376e00852fe803b4))
* **deps:** update scalamock to v0.6.6 ([bfde7a6](https://github.com/position-pal/location-service/commit/bfde7a6c0388ca4daad2efc224bf8d659316efbb))

### Bug Fixes

* **ws:** make synchronized access to active ws session ([1af295e](https://github.com/position-pal/location-service/commit/1af295ef1a3e3076e9bd1d802594334503bbadc3))

### Documentation

* add scala doc to classes ([ebe805d](https://github.com/position-pal/location-service/commit/ebe805dc6cf30a35973953b0e4f93a32e358229d))

### Tests

* **infrastructure:** do not ignore test ([4fdf4f8](https://github.com/position-pal/location-service/commit/4fdf4f825b724a9e7475b456427d94ba5e38e186))
* **infrastructure:** increase interval span and leverage on the usage of system resource ([545d07a](https://github.com/position-pal/location-service/commit/545d07a18896cc661585734e9884e13c116c7f07))

### Build and continuous integration

* add docker image build and publication ([dfd8c1c](https://github.com/position-pal/location-service/commit/dfd8c1cca1a60964e04f3e08719bccf1b56af434))
* do not inject environment variables from .env if the file is not present ([5ea9642](https://github.com/position-pal/location-service/commit/5ea9642cc6a0fe3c81057ff03655a8f1d825d30c))
* fix arguments order in contains function ([9f195a7](https://github.com/position-pal/location-service/commit/9f195a7efd7545cee5a2997d8722672c2a82405b))
* remove useless `DOCKER_USERNAME` environment variable from relase job ([310a3ee](https://github.com/position-pal/location-service/commit/310a3eed0b978b79ea81c5ae4b9b7d1bd8768152))

### General maintenance

* add entrypoint ([3439243](https://github.com/position-pal/location-service/commit/34392436fd2de2b15a4d276b5de169ff410bf647))
* add projection skeleton ([87330fc](https://github.com/position-pal/location-service/commit/87330fc0c79077ee75f04b8cfd8cad2c1862ec0c))

### Refactoring

* **application:** add `Session.Snapshot`, position in `RoutingStarted` and `userId` in Session ([4c339ae](https://github.com/position-pal/location-service/commit/4c339ae7ed14b0623fcc2bbadff57b96feff05f6))
* **domain:** remove redundant userId in tracking since already present in session ([ce5bedf](https://github.com/position-pal/location-service/commit/ce5bedf56f97dfe24d3434af3cc58f66df50d214))
* **infrastructure:** leverage session in actor state ([91aeafa](https://github.com/position-pal/location-service/commit/91aeafac1ae18ad2b27be369cc4ae1aa16c8fb54))
* **infrastructure:** support snapshotting and failure strategy in persistent actor ([f232181](https://github.com/position-pal/location-service/commit/f2321810763ff7fca19eb28276df706ff89316f3))
* introduce session aggregate ([5efbdf0](https://github.com/position-pal/location-service/commit/5efbdf0b82bc825da3ecb619414fed2ff1701b6f))
* remove not used code and dependencies ([f40ed76](https://github.com/position-pal/location-service/commit/f40ed763738f5689cbd90b1fe24d838c0a35f3fb))
* **storage:** switch to cassandra data store ([07b5056](https://github.com/position-pal/location-service/commit/07b50563b9518a53d843d6206edac206544705ee))
* use Instance in place of Date ([69b82f9](https://github.com/position-pal/location-service/commit/69b82f97f5ce86b511a34336d6f90651c55fc0c8))

## [0.5.0](https://github.com/position-pal/location-service/compare/0.4.0...0.5.0) (2024-10-31)

### Features

* **infrastructure:** add web socket service ([#67](https://github.com/position-pal/location-service/issues/67)) ([319d1c2](https://github.com/position-pal/location-service/commit/319d1c207d89bdb06dd662ec79d69be5b6ce071e))

### Dependency updates

* **deps:** pin postgres docker tag to 6b3d44b ([e6cdaf5](https://github.com/position-pal/location-service/commit/e6cdaf513dcbcdb965e3e657e06af24a7832579d))
* **deps:** update akka to v2.10.0 ([f24a79d](https://github.com/position-pal/location-service/commit/f24a79dc01fc243b718dc2de44399a015732c3b1))
* **deps:** update akka to v2.9.7 ([73b9e03](https://github.com/position-pal/location-service/commit/73b9e03559e36f1ce70e0eb3c49daae494b30fed))
* **deps:** update dependency ch.qos.logback:logback-classic to v1.5.11 ([18e18d6](https://github.com/position-pal/location-service/commit/18e18d68c4d700e3ebd0dd5ae9f5466d77ca3082))
* **deps:** update dependency ch.qos.logback:logback-classic to v1.5.12 ([f4b56e1](https://github.com/position-pal/location-service/commit/f4b56e1d490b8a02f048d02dd7730405e6796967))
* **deps:** update dependency com.lightbend.akka:akka-persistence-r2dbc_3 to v1.2.6 ([2b5e07b](https://github.com/position-pal/location-service/commit/2b5e07b6c1081571af044e1d1295198166bd74c3))
* **deps:** update dependency com.lightbend.akka:akka-persistence-r2dbc_3 to v1.3.0 ([186a492](https://github.com/position-pal/location-service/commit/186a49237877cb8cebeb142c638ff3e1b78b303c))
* **deps:** update dependency io.cucumber:cucumber-scala_3 to v8.25.1 ([dd83ea0](https://github.com/position-pal/location-service/commit/dd83ea0cb64d4e84bf9bf43a4715522680d1ffc9))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.112 ([104d4d4](https://github.com/position-pal/location-service/commit/104d4d4bdf98f8e4cd30a15dd192f99695466b75))
* **deps:** update junit5 monorepo to v1.11.3 ([f44a86a](https://github.com/position-pal/location-service/commit/f44a86ab499c4a55374bc0abd75fbdc0768a839a))
* **deps:** update postgres:17.0 docker digest to 8d3be35 ([2cad31b](https://github.com/position-pal/location-service/commit/2cad31be2ca189906e45d920cb62db35f9613932))

### Build and continuous integration

* **deps:** update actions/checkout digest to 11bd719 ([f0cf486](https://github.com/position-pal/location-service/commit/f0cf486a6763c744fa9621e607c566f300c58f07))
* **deps:** update actions/setup-java digest to 8df1039 ([9218648](https://github.com/position-pal/location-service/commit/921864852ccc665079c4aef85fd2eac0a23fbebb))
* **deps:** update actions/setup-node action to v4.1.0 ([1811e27](https://github.com/position-pal/location-service/commit/1811e2792602b1115be63120237943350f9f833b))
* move .env management to buildSrc ([#56](https://github.com/position-pal/location-service/issues/56)) ([c681774](https://github.com/position-pal/location-service/commit/c6817741d0323a39dec43933718062f33c937d30))

## [0.4.0](https://github.com/position-pal/location-service/compare/0.3.0...0.4.0) (2024-10-17)

### Features

* **infrastructure:** add real-time location tracking service ([#48](https://github.com/position-pal/location-service/issues/48)) ([ef51fb9](https://github.com/position-pal/location-service/commit/ef51fb9a43b766abe7946553a82c0448b7bc69b8))

### Dependency updates

* **deps:** update circe to v0.14.10 ([0269f17](https://github.com/position-pal/location-service/commit/0269f1796c1ef09465f521088194a56e1dae2988))
* **deps:** update dependency gradle to v8.10.1 ([a792cbb](https://github.com/position-pal/location-service/commit/a792cbb65d4dfd040337b4aff1bdfd2257e282b0))
* **deps:** update dependency gradle to v8.10.2 ([afb44a6](https://github.com/position-pal/location-service/commit/afb44a6a1fe508cd8b633a0dcf25bf60835fe94d))
* **deps:** update dependency org.scala-lang:scala3-library_3 to v3.5.1 ([f15d38c](https://github.com/position-pal/location-service/commit/f15d38cfb505b2ee3f66bc93642d7ee81661a744))
* **deps:** update dependency org.scala-lang:scala3-library_3 to v3.5.2 ([c85bb6c](https://github.com/position-pal/location-service/commit/c85bb6cb41368164a80466038187ee441c8fc244))
* **deps:** update dependency org.scalatestplus:junit-5-10_3 to v3.2.19.1 ([7bae24a](https://github.com/position-pal/location-service/commit/7bae24a96ea36a56d5abab54eb8776627e73020e))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.102 ([16c2c5e](https://github.com/position-pal/location-service/commit/16c2c5e1f87a778123b0c8518423e06d62436787))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.103 ([4875fb3](https://github.com/position-pal/location-service/commit/4875fb3d248549a9139a7dfee9a7ef81feb96248))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.104 ([4f1d1fb](https://github.com/position-pal/location-service/commit/4f1d1fbf2850be088947cb8c49b4c7adbf42b4bd))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.105 ([2653d69](https://github.com/position-pal/location-service/commit/2653d6983efbf87fbab2c3e23fef2731d60aa9d1))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.106 ([2fd5a0a](https://github.com/position-pal/location-service/commit/2fd5a0acce2196148771a25f1dd5819488df5283))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.107 ([5ef4182](https://github.com/position-pal/location-service/commit/5ef41821984fa166a5905d26acf63ffd236cf7d1))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.109 ([02d2151](https://github.com/position-pal/location-service/commit/02d2151563193d23914cfaebfa8292eb64d28ec6))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.110 ([b06f067](https://github.com/position-pal/location-service/commit/b06f067fda11346326f680556717170596485331))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.111 ([29c0614](https://github.com/position-pal/location-service/commit/29c0614d7d50e8a4bde1eb6e240b1e3954ab2aa0))
* **deps:** update http4s to v1.0.0-m42 ([c16906e](https://github.com/position-pal/location-service/commit/c16906e1c397464b856b6d6dc092cbc1b0a1e436))
* **deps:** update junit5 monorepo to v1.11.1 ([6ae9084](https://github.com/position-pal/location-service/commit/6ae90841386fff06c45a2fb76ee7d0ef3a9795d6))
* **deps:** update junit5 monorepo to v1.11.2 ([2d357b3](https://github.com/position-pal/location-service/commit/2d357b3e89387c0adfc81655b1568710ee919603))
* **deps:** update node.js to 20.18 ([84ad310](https://github.com/position-pal/location-service/commit/84ad3105b306398ceab23edcf958a24de43fde84))
* **deps:** update plugin com.gradle.develocity to v3.18.1 ([e24568c](https://github.com/position-pal/location-service/commit/e24568cf8fb3d5febbd2ed7d04279f060527d349))
* **deps:** update plugin org.danilopianini.gradle-pre-commit-git-hooks to v2.0.12 ([eaf36ce](https://github.com/position-pal/location-service/commit/eaf36ce509739c11a9b986467b666667427fe221))
* **deps:** update plugin org.danilopianini.gradle-pre-commit-git-hooks to v2.0.13 ([e550df6](https://github.com/position-pal/location-service/commit/e550df613ed7df0e6acbc5487c3b07b73d9ccdff))
* **deps:** update plugin scala-extras to v2.1.2 ([0550312](https://github.com/position-pal/location-service/commit/05503123038db3b522487d7686644c73f7f953f8))
* **deps:** update plugin scala-extras to v2.1.3 ([679409b](https://github.com/position-pal/location-service/commit/679409b1c95a03f47aaf8b109d0dce315399f9e4))
* **deps:** update plugin scala-extras to v2.1.4 ([3f85b84](https://github.com/position-pal/location-service/commit/3f85b84d0a9b4d167c7dcf80ed8b8a581467e171))
* **deps:** update scalamock to v0.6.5 ([8ff7d6a](https://github.com/position-pal/location-service/commit/8ff7d6a9739ccd1e0b77d2a6f79f45db2d670ff0))

### Tests

* **infrastructure:** relax threshold of distance ([0f8a054](https://github.com/position-pal/location-service/commit/0f8a05444c344e7e3f033680bbc6a655da38ec6d))

### Build and continuous integration

* **deps:** update actions/checkout digest to eef6144 ([bc4f171](https://github.com/position-pal/location-service/commit/bc4f1711db334a7ef80ca8bd1ffd5c2426b496b5))
* **deps:** update actions/setup-java digest to 2dfa201 ([3306c9a](https://github.com/position-pal/location-service/commit/3306c9a647ba16befa1259c1db0e9b471fe90db3))
* **deps:** update actions/setup-java digest to b36c23c ([1782cd9](https://github.com/position-pal/location-service/commit/1782cd983daef4fde7cfa1d0fce077dd9086ff97))
* **deps:** update actions/setup-node action to v4.0.4 ([a3f03f7](https://github.com/position-pal/location-service/commit/a3f03f7d83ed435dbf7852120795a930293ccbbd))
* **deps:** update dependency ubuntu to v24 ([933f2af](https://github.com/position-pal/location-service/commit/933f2af6159692f7f6ecb91582d20803eafa0749))

## [0.3.0](https://github.com/position-pal/location-service/compare/0.2.0...0.3.0) (2024-09-03)

### Features

* **routes:** add routes and events reaction mechanism ([#11](https://github.com/position-pal/location-service/issues/11)) ([32e34a1](https://github.com/position-pal/location-service/commit/32e34a1ea7c9db01cab27d4303720613e62569b9))

### Dependency updates

* **deps:** update plugin com.gradle.develocity to v3.18 ([2e1c51b](https://github.com/position-pal/location-service/commit/2e1c51b1be0f211367b9e2e534693fdf86d8b202))
* **deps:** update plugin org.danilopianini.gradle-pre-commit-git-hooks to v2.0.9 ([88e0163](https://github.com/position-pal/location-service/commit/88e016315d660a1e5c0e8295e36530cba97ae0bd))

### Build and continuous integration

* **presentation:** configure akka grpc ([c98d676](https://github.com/position-pal/location-service/commit/c98d676584b2357cd20d8e26f2b9276b324f7bef))

## [0.2.0](https://github.com/position-pal/location-service/compare/0.1.0...0.2.0) (2024-08-29)

### Features

* **maps:** add maps service and associated domain models ([#3](https://github.com/position-pal/location-service/issues/3)) ([1539f34](https://github.com/position-pal/location-service/commit/1539f34cc281131a1c39dccc23dbb83a440c4a32))

### Dependency updates

* **deps:** update dependency org.typelevel:cats-effect_3 to v3.5-6581dc4 ([11e2fd4](https://github.com/position-pal/location-service/commit/11e2fd4a4f223aa28c09a773c89a5e897d6a940d))
* **deps:** update dependency org.typelevel:cats-effect_3 to v3.6-623178c ([f983b9e](https://github.com/position-pal/location-service/commit/f983b9ee46cf1f61d5a246a0e024967552cfe15e))
* **deps:** update dependency org.typelevel:cats-mtl_3 to v1.5.0 ([d2a9b25](https://github.com/position-pal/location-service/commit/d2a9b25cc5e445b222b4b78d36c0f7131a2a9a7f))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.100 ([59500b0](https://github.com/position-pal/location-service/commit/59500b035ac5796df25e6fe609fa175db0e83070))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.101 ([e7468fc](https://github.com/position-pal/location-service/commit/e7468fc2d47a6afbe81d96e89784904349cf65ff))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.98 ([5f88a85](https://github.com/position-pal/location-service/commit/5f88a855d6a30b548748e62ca479fa90da813127))
* **deps:** update dependency semantic-release-preconfigured-conventional-commits to v1.1.99 ([b8e637e](https://github.com/position-pal/location-service/commit/b8e637e9b1deab9b4e08241f5831223de01b8840))
* **deps:** update node.js to 20.17 ([d180488](https://github.com/position-pal/location-service/commit/d180488321a38d38eddecae1cbf2ac478e983a0c))
* **deps:** update plugin scala-extras to v2.1.1 ([39efceb](https://github.com/position-pal/location-service/commit/39efcebe52851dabb391ba942a74317c376021bc))

### General maintenance

* add .env file to gitignore ([fa8ec09](https://github.com/position-pal/location-service/commit/fa8ec097ffeda1caee8c085f72f907f55546533d))
* **release:** 1.0.0 [skip ci] ([1c54050](https://github.com/position-pal/location-service/commit/1c54050bb7a9f25420769c97d53144d00a56e741)), closes [#3](https://github.com/position-pal/location-service/issues/3)
