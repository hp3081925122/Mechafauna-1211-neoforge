# Mechafauna 迁移至 Minecraft 1.20.1 Forge

## 目标与原则

把现有 1.21.1 NeoForge 版本完整迁移为独立的 1.20.1 Forge 工程，保留实体、AI、战斗、动画、物品、盔甲、结构生成、战利品、配方、语言、粒子、音效和客户端交互。此次迁移不得通过删功能或弱化机制来换取编译通过。原 1.21.1 NeoForge 工程保持不动。

本文档先于模组源码迁移建立，作为迁移范围、接口替换依据和验收清单。遇到版本差异时，以目标工程 Forge 47.4.23 及其 Minecraft 1.20.1 依赖中的真实 API 为准；本机 47.4.20 源码仅作源码导航，最终以目标工程编译结果复核。

## 已核对的基线

| 项目 | 当前源版本 | 目标版本 | 核对依据 |
| --- | --- | --- | --- |
| Minecraft / 加载器 | 1.21.1 NeoForge 21.1.233 | 1.20.1 Forge 47.4.23 | 两侧 `gradle.properties` 与版本模板 |
| Java | 21 | 17 | 两侧构建配置与模板 README |
| Gradle | 8.14 | 8.8 | 两侧 Wrapper 属性 |
| 映射 | 未显式声明，使用 NeoForge 模板默认配置 | Forge 官方映射 `1.20.1` | 目标 Forge 模板 `build.gradle` / `gradle.properties` |
| 模组标识 | `mechafauna`，包 `com.hp.mechafauna` | 保持不变 | 源 `gradle.properties` 与源码包路径 |
| 版本号 | `1.21.1-neoforge-1.0.0` | `1.20.1-forge-1.0.0` | 版本分支命名规则 |
| 源码规模 | 66 个主源码 Java 文件；另有测试源码 | 全量迁移 | 源 `src` 清单 |
| 资源规模 | 125 个主资源文件 | 全量迁移并核查格式兼容 | 源 `src/main/resources` 清单 |

目标工作区从 `E:\模组\模组模板\1201-forge` 创建。模板使用 ForgeGradle `[6.0,6.2)`、官方映射和 Java 17；不得从 1.21.1 NeoForge 工程复制其 Gradle 插件配置来替代目标模板。

## 必须保持的功能契约

- 保留现有全部实体注册、属性、AI、乘骑、目标选择、伤害判定、首领阶段与武器部件机制。
- 保留无人机手动控制、玩家乘骑和飞行逻辑；服务端仍须校验实体类型、所有权和当前乘客关系。
- 保留火力支援、机枪姿势、机甲盔甲、棱光镜核心、机械犬召唤物及现有战斗效果。
- 保留结构与世界生成、结构战利品、实体战利品、配方、伤害类型、标签、声音、粒子、GeckoLib 模型和动画、中英文语言文件。
- 保留客户端按键、渲染器、Boss 血条和音乐等客户端行为，并确保这些类只在客户端加载。
- 保持资源命名空间和公开注册 ID 为 `mechafauna`；不要无必要重命名物品、实体、结构、数据键或存档标识。

## 已发现的版本耦合与迁移动作

| 区域 | 源工程证据 | 迁移动作与验收 |
| --- | --- | --- |
| 启动与事件总线 | `Mechafauna.java` 注入 NeoForge `IEventBus`、`ModContainer`，并注册多个监听器 | 改为 Forge 47.4.23 构造方式和对应的 mod / Forge 全局事件总线；逐个核对监听器生命周期和物理侧 |
| 注册系统 | 注册器中使用 NeoForge `DeferredHolder`、`DeferredRegister.createItems` 和 NeoForge 扩展注册 | 使用目标 Forge 1.20.1 中经源码确认的 `DeferredRegister` / 注册对象写法；所有注册 ID 和对象行为保持一致 |
| 创造栏、实体属性、粒子、声音 | 使用 NeoForge 专属事件/注册包 | 映射到 Forge 对应事件和注册 API；确认物品栏可见性、属性绑定、粒子数据与声音注册均生效 |
| 网络 | `DroneControlPayload`、`FireSupportPayload` 使用新版 `CustomPacketPayload`、`StreamCodec` 和 NeoForge `PayloadRegistrar` | 按 Forge 1.20.1 的频道 API 重写传输层；包体字段、方向、线程调度、协议版本、服务端所有权校验和释放控制语义必须保持 |
| 机枪手臂姿势 | `enumextensions.json` 给 `HumanoidModel.ArmPose` 增加 `MECHAFAUNA_MACHINE_GUN`，依赖 NeoForge `IArmPoseTransformer` | 删除目标版本不支持的 NeoForge enum 扩展配置；以 Forge 1.20.1 源码确认的渲染事件或 Mixin 保留同样的持枪姿势，不得仅移除功能 |
| 玩家无人机姿势 | `mechafauna.mixins.json` 注册 `PlayerDronePoseMixin`，目标为 `PlayerModel.setupAnim` | 保留 Mixin 配置和元数据注册；将 Java 21 兼容级调整为 Java 17，并按 Forge 1.20.1 目标源码核对方法描述符、注入点和映射，之后检查实际客户端加载 |
| NeoForge 模组元数据 | 当前为 `META-INF/neoforge.mods.toml`，声明 NeoForge、Minecraft、GeckoLib 和 enum 扩展 | 用模板的 `META-INF/mods.toml`，填写 Forge/Minecraft/GeckoLib 依赖范围；禁止把 NeoForge 标识或 enum 扩展残留打入产物 |
| 配置 | `MechafaunaConfig` 使用 NeoForge `ModConfigSpec` | 迁移到 Forge 1.20.1 配置 API，并保持配置键、默认值、范围和生效侧不变 |
| GeckoLib | 源工程依赖 NeoForge 变体 4.8.4 | 目标使用已核验的 Forge 1.20.1 Release 4.8.4（CurseForge file ID 8285794），坐标 `software.bernie.geckolib:geckolib-forge-1.20.1:4.8.4`；逐个核对实体、物品、盔甲、渲染器和动画控制器 API |
| Jade | 当前 Gradle 声明了 Jade，但主源码搜索未发现 Jade API 引用 | 迁移时不盲目复制 1.21.1 NeoForge 文件；确认完整源码无调用后不带入该无用依赖 |
| Java / MC API | 源工程按 Java 21 和 Minecraft 1.21.1 编译 | 使用 Java 17；按目标源码适配方法签名、注册和实体行为，不删对应玩法 |
| 资源定位 | 多处调用 `ResourceLocation.fromNamespaceAndPath` | 按目标 1.20.1 API 替换构造和解析调用，并通过编译核验 |
| 数据资源目录 | 源版使用 `loot_table/`、`worldgen/structures/`、`structure/` | Forge 1.20.1 目标分别使用 `loot_tables/`、`worldgen/structure/`、`structures/`；映射后逐文件核对并由结构/战利品 GameTest 验证 |
| 手持机枪姿势 | 源版通过 NeoForge enum 扩展注册自定义 `ArmPose` | 移除 `enumextensions.json`，在 Forge `IClientItemExtensions` 中以 `HumanoidModel.ArmPose.create` 和 Forge `IArmPoseTransformer` 保留等效姿势；目标版已编译并由客户端加载 |
| 物品数据 | `ReinforcementCoreItem` 用 `DataComponents.CUSTOM_DATA` / `CustomData` 保存状态 | 用 1.20.1 ItemStack NBT 保存同一键和值，保留重复使用/召唤次数等语义 |
| 自定义粒子网络序列化 | `ExplosionParticleOptions` 和 `ModParticles` 使用 `MapCodec`、`StreamCodec`、`RegistryFriendlyByteBuf` | 使用目标版 `ParticleOptions` 与其反序列化器/`FriendlyByteBuf` API，保持编码字段和值一致 |
| 盔甲与实体缩放 | `ModArmorMaterials` 使用 1.21 `ArmorItem.Type` / `ArmorMaterial.Layer` 并声明 `Type.BODY`；`MechanicalTank` 使用 `Attributes.SCALE` | 改写为 1.20.1 盔甲材质和槽位 API；机械坦克视觉缩放、碰撞尺寸与战斗距离须分别保留并以源码/运行核验 |
| 实体乘客位置 | 机械狼和无人机注册时调用 `EntityType.Builder.passengerAttachments` | 在目标版实体乘客定位回调中保留三种实体各自的偏移量，并核查客户端模型姿势 |
| 游戏测试 | 源版有 GameTest 与武器瞄准检查 | 改为 Forge 1.20.1 可运行的 GameTest；结构旋转、实体生成/边界、刷怪笼与掉落表共 9 项必需测试通过；瞄准检查 6,046 项通过 |

## 迁移顺序

1. 保留源工程原样；从 1.20.1 Forge 官方模板建立独立目标工程。
2. 将本项目的迁移文档放入目标工程后，再复制完整 `src` 源码和资源；排除源工程的 `.git`、`.gradle`、`build`、IDE 状态、运行目录、日志和存档。
3. 先迁移 Gradle 属性、项目名、Forge 元数据、Mixin 注册、依赖和资源包格式，再按上表逐层迁移 Java API。
4. 对网络、注册、GeckoLib、客户端渲染/Mixin、GameTest 和所有静态数据执行完整清点；每修复一类接口，都回查相应玩法契约。
5. 搜索残留的 `net.neoforged`、NeoForge 元数据、enum 扩展、1.21 专属类名及模板示例标识，清理不再有效的入口。
6. 用 Java 17 与目标 Wrapper 执行 `compileJava`、`processResources`，再生成正式 JAR；有编译错误时按目标依赖源码修复，而不是降低功能范围。
7. 运行目标客户端核对模组加载、Mixin、手持动画、实体注册、按键和 GeckoLib 渲染；验证无人机控制及火力支援的客户端到服务端流程，并验证结构/掉落等数据内容。

## 完成判定

- [x] 目标工程以 Java 17、Forge 47.4.23、Minecraft 1.20.1 完成 `build` 和正式 JAR 构建。
- [x] JAR 只含 Forge `META-INF/mods.toml`；包内未发现 NeoForge 元数据、enum 扩展配置或 `net/neoforged` 路径。
- [x] 实体、物品、语言键、模型、动画、结构、掉落表和其他资源已清点；中英文各 53 个语言键均与源版对应。
- [x] Forge 1.20.1 GameTest 的 9 项必需测试通过，`verifyWeaponAim` 的 6,046 项检查通过。
- [x] 开发客户端成功加载模组并进入集成世界；日志确认 `PlayerDronePoseMixin` 与 `SaucerBossMusicMixin` 已应用，生物 AI、机枪/导弹发射、弹丸与爆炸逻辑均在服务器线程运行；客户端随后正常保存世界并以退出码 0 结束，没有新增崩溃报告。
- [x] 源 1.21.1 NeoForge 工程的 `git status --short` 为空，迁移工作位于独立 Forge 项目。

## 迁移记录

- 2026-09-27：完成基线盘点，并先于源码迁移在目标模板内建立本迁移文档；这是迁移开始时的阶段记录。

## 已完成记录

- 2026-09-27：目标模板建立并完成首轮源码/资源复制；当时保留 Forge `mods.toml`，版本 API 适配尚未开始，后续阶段结果见下文。
- 2026-09-27：核验 GeckoLib Forge 1.20.1 4.8.4 发布文件 ID 8285794；当前 Java 源中无 Jade API 调用，目标配置不带入 Jade 依赖。
- 2026-09-27：完成注册、Forge 网络频道、实体/物品/盔甲客户端扩展、伤害/声音/耐久、乘客位置、1.20 投射物重力、实体零件和 GameTest API 的首轮适配；将 NeoForge 分视角物品模型 loader 改为 Forge 同名 loader，保留 GUI 图标与 3D 手持模型。
- 2026-09-27：将 1.21 自定义爆炸计算器适配为 Forge 1.20.1 实体爆炸处理，保留范围/遮挡伤害、爆炸抗性、每种武器的击退倍率、蜘蛛召唤者与玩家保护、Forge 爆炸事件、自定义粒子和声音。
- 2026-09-27：按 1.20.1 格式迁移战利品表、结构 NBT 与 worldgen 结构目录；用实际 Forge GameTest 验证四向结构边界、结构实体、坦克碰撞尺寸、刷怪笼及掉落表，9 项必需测试全部通过。
- 2026-09-27：JDK 17 / Gradle 8.8 执行 `build` 成功；`verifyWeaponAim` 通过 6,046 项检查；正式产物为 `build/libs/mechafauna-1.20.1-forge-1.0.0.jar`（约 2.96 MB）。
- 2026-09-27：开发客户端在 Forge 47.4.23 / Minecraft 1.20.1 成功加载模组并进入集成世界；两个模组 Mixin 均已应用，集成服务器运行生物攻击与投射物逻辑后正常保存退出，无新客户端崩溃报告。

## 当前状态复核

- 目标工程：`E:\模组\发cf的模组\生物\机械\Mechafauna-1201-forge`，独立于原 1.21.1 NeoForge 工程；目标加载器 Forge 47.4.23，Minecraft 1.20.1，Java 17，版本 `1.20.1-forge-1.0.0`。
- 源版 66 个 Java 主源码中，65 个保留相同相对路径；机枪姿势扩展整合进 Forge 版 `HeavyMechMachineGunItem`。Forge 目标共 69 个主源码文件，新增 Forge 网络/客户端适配、结构 GameTest 辅助和首领音乐 Mixin。目标主资源 124 个；资源目录迁移映射后无缺失项，唯一移除的是已由 Forge 物品扩展替代的 NeoForge `META-INF/enumextensions.json`。
- JAR 清点确认 Forge 元数据、Mixin、两种语言、全部实体/物品资源、`loot_tables/`、`structures/` 与 `worldgen/structure/` 均已打包；未发现 NeoForge 专属元数据或类路径。
- 首次迁移编译由 273 个错误逐步收敛为零；当前完整构建成功。仍有 Gradle/第三方 API 弃用提示，不影响本次构建和运行。
- 客户端开发测试用的是 `run/saves/新的世界`；该存档由运行测试保留，未重置。GameTest 生成的旧 `run/world` 已完整保留在 `D:\AI临时文件\mechafauna-1201-run-world-before-manual-client`。
- 客户端启动曾记录 Yggdrasil 离线认证连接超时；这是网络警告。测试客户端正常退出，没有生成新的崩溃报告。
