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
