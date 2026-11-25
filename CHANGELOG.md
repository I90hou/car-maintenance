# Changelog

本项目的所有显著变更都将记录在本文件。

格式基于 [Keep a Changelog 1.1.0](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

> 本文件从 v1.1.0 之后的改动开始记录，更早的发布未纳入。

## [Unreleased]

### Changed

- 界面补上一整套语义图标：保养项目按类型显示专属图标（机油、空气滤芯、空调滤芯、滤清器、刹车片、变速箱油、蓄电池、轮胎、雨刮、防冻液等），记录时间线按类型区分保养与维修，各页面分区标题与空状态也各配图标；图标只负责「这是什么」，颜色继续负责「状态如何」
- 车辆线稿重画为三厢比例，两轮共用地平线、车窗落在柱内，不再歪斜
- 启动图标重画，此前车身被圆形遮罩裁掉了车头与车尾
- 自适应图标补上单色层，支持 Android 13+ 主题图标

### Fixed

- 记录列表里的「删除」先弹出确认框，不再一点即删
- 编辑一条记录后，详情页的零配件明细不再清零
- 设置页的「展开配置」现在能真正展开 WebDAV 字段
- 记录时间线上的连接线不再在每个节点处断开

[Unreleased]: https://github.com/uwakeme/car-maintenance/commits/main
