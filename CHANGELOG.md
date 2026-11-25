# Changelog

本项目的所有显著变更都将记录在本文件。

格式基于 [Keep a Changelog 1.1.0](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

> 本文件从 v1.2.0 开始记录；v1.1.0 及更早的发布未纳入。

## [Unreleased]

## [1.2.0] - 2026-11-25

### Added

- 保养周期推算：按项目设定里程与月份周期，概览页据此推算各部件的下一次到期点、剩余量与逾期天数 ([54bf72f])

### Changed

- 界面重构为「现代车座舱」：全暗底、近乎无彩，只留一条电光蓝强调色 ([54bf72f])
- 界面补上一整套语义图标：保养项目按类型显示专属图标（机油、空气滤芯、空调滤芯、滤清器、刹车片、变速箱油、蓄电池、轮胎、雨刮、防冻液等），记录时间线按类型区分保养与维修，分区标题与空状态也各配图标；图标只负责「这是什么」，颜色继续负责「状态如何」 ([1736838])
- 车辆线稿重画为三厢比例，两轮共用地平线、车窗落在柱内，不再歪斜 ([1736838])
- 启动图标重画，此前车身被圆形遮罩裁掉了车头与车尾 ([1736838])
- 自适应图标补上单色层，支持 Android 13+ 主题图标 ([1736838])

### Fixed

- 记录时间线上补上保养地点，不再只有里程和日期 ([781096e])
- 记录列表里的「删除」先弹出确认框，不再一点即删 ([1736838])
- 编辑一条记录后，详情页的零配件明细不再清零 ([1736838])
- 设置页的「展开配置」现在能真正展开 WebDAV 字段 ([1736838])
- 记录时间线上的连接线不再在每个节点处断开 ([1736838])

[Unreleased]: https://github.com/uwakeme/car-maintenance/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/uwakeme/car-maintenance/compare/v1.1.0...v1.2.0
[54bf72f]: https://github.com/uwakeme/car-maintenance/commit/54bf72f1ad9bbe6041c5c839f58520ef6fa20a21
[781096e]: https://github.com/uwakeme/car-maintenance/commit/781096ee9a0c7496898a2ff70eeb8f1777b87f04
[1736838]: https://github.com/uwakeme/car-maintenance/commit/17368386455a94e36db2f57dd238310ded5c8b5a
