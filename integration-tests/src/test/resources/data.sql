-- Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.

INSERT INTO
  parana_users
  (`id`, `name`, `email`, `mobile`, `password`, `type`, `status`, `roles_json`, `extra_json`, `tags_json`, `created_at`, `updated_at`)
VALUES
  (1, 'admin','admin@terminus.io', '18888888888', '9f8c@a97758b955efdaf60fe4', 1, 1, null, null, '{"good":"man"}', now(), now());

INSERT INTO `parana_message_templates` (`id`, `creator_id`, `creator_name`, `name`, `title`, `content`, `context`, `channel`, `disabled`, `description`, `created_at`, `updated_at`)
VALUES
	(1, 1, 'admin', 'user.register.code', '用户中心手机注册码', '您的注册手机验证码是：{{code}}', '{"code":"123456"}', 1, 0, '用户中心手机注册码', '2016-05-17 17:08:43', '2016-05-17 17:08:43');


INSERT INTO `parana_configs` (`biz_type`, `key`, `value`, `data_type`, `group`, `description`, `created_at`, `updated_at`)
VALUES
	(0, 'msg.umeng.appKey', 'appKey', 'string', '0', 'app消息推送网关umeng的key', now(), now()),
	(0, 'msg.umeng.appSecret', 'appSecret', 'string', '0', 'app消息推送网关umeng的secret', now(), now()),
	(0, 'msg.sendcloud.user', 'user', 'string', '0', '邮件网关sendcloud的user', now(), now()),
	(0, 'msg.sendcloud.batchUser', 'batchUser', 'string', '0', '邮件网关sendcloud的batchUser', now(), now()),
	(0, 'msg.sendcloud.triggerUser', 'triggerUser', 'string', '0', '邮件网关sendcloud的triggerUser', now(), now()),
	(0, 'msg.sendcloud.key', 'key', 'string', '0', '邮件网关sendcloud的key', now(), now()),
	(0, 'msg.alisms.appKey', 'appKey', 'string', '0', '短信网关alidayu的key', now(), now()),
	(0, 'msg.alisms.appSecret', 'appSecret', 'string', '0', '短信网关alidayu的secret', now(), now()),
	(0, 'msg.current.email.service', 'sendCloudEmailService', 'string', '0', '当前的邮件网关', now(), now()),
	(0, 'msg.current.sms.service', 'aliSmsService', 'string', '0', '当前的短信网关', now(), now()),
	(0, 'msg.current.app.push.service', 'umengAppPushService', 'string', '0', '当前的app消息推送网关', now(), now());

insert into `parana_configs`
(`biz_type`, `key`, `value`, `data_type`, `group`, `description`, created_at, updated_at)
values
(0, 'system.pigmall.password', 'pigmall', 'string', 0, '第三方用户登录pigmall系统的接口密码', NOW(), now()),
(0, 'system.pigmall.corp.id', '1', 'string', 0, '本系统在pigmall系统的corp_id', NOW(), now()),
(0, 'system.pigmall.domain', 'http://www.pigmall.com', 'string', 0, 'pigmall系统的完整域名', NOW(), now()),
(0, 'system.neverest.password', 'neverest', 'string', 0, '第三方用户登录neverest系统的接口密码', NOW(), now()),
(0, 'system.neverest.corp.id', '1', 'string', 0, '本系统在neverest系统的corp_id', NOW(), now()),
(0, 'system.neverest.domain', 'http://www.neverest.com', 'string', 0, 'neverest系统的完整域名', NOW(), now());

-- warehouse content
-- ware house type
INSERT INTO `doctor_farm_ware_house_types` (`id`, `farm_id`, `farm_name`, `type`, `log_number`, `extra`, `creator_id`, `creator_name`, `updator_id`, `updator_name`, `created_at`, `updated_at`)
VALUES
	(1, 12345, 'testFarm', 1, 10000000, NULL, 1, 'creator', 1, 'updator', '2016-05-25 12:34:17', '2016-05-25 12:34:17'),
	(2, 12345, 'testFarm', 2, 10000000, NULL, 1, 'creator', 1, 'updator', '2016-05-25 12:34:17', '2016-05-25 12:34:17'),
	(3, 12345, 'testFarm', 3, 10000000, NULL, 1, 'creator', 1, 'updator', '2016-05-25 12:34:17', '2016-05-25 12:34:17'),
	(4, 12345, 'testFarm', 4, 10000000, NULL, 1, 'creator', 1, 'updator', '2016-05-25 12:34:17', '2016-05-25 12:34:17'),
	(5, 12345, 'testFarm', 5, 10000000, NULL, 1, 'creator', 1, 'updator', '2016-05-25 12:34:17', '2016-05-25 12:34:17');

-- doctor_ware_houses
INSERT INTO `doctor_ware_houses` (`id`, `ware_house_name`, `farm_id`, `farm_name`, `manager_id`, `manager_name`, `address`, `type`, `extra`, `creator_id`, `creator_name`, `updator_id`, `updator_name`, `created_at`, `updated_at`)
VALUES
	(1, 'wareHouseName1', 12345, 'farmIdName', 1, 'managerName', 'addressName', 1, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(2, 'wareHouseName2', 12345, 'farmIdName', 1, 'managerName', 'addressName', 1, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(3, 'wareHouseName3', 12345, 'farmIdName', 1, 'managerName', 'addressName', 2, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(4, 'wareHouseName4', 12345, 'farmIdName', 1, 'managerName', 'addressName', 2, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(5, 'wareHouseName5', 12345, 'farmIdName', 1, 'managerName', 'addressName', 3, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(6, 'wareHouseName6', 12345, 'farmIdName', 1, 'managerName', 'addressName', 3, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(7, 'wareHouseName7', 12345, 'farmIdName', 1, 'managerName', 'addressName', 4, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(8, 'wareHouseName8', 12345, 'farmIdName', 1, 'managerName', 'addressName', 4, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(9, 'wareHouseName9', 12345, 'farmIdName', 1, 'managerName', 'addressName', 5, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10'),
	(10, 'wareHouseName10', 12345, 'farmIdName', 1, 'managerName', 'addressName', 5, NULL, 1, 'craetorName', 1, 'updateName', '2016-06-02 16:01:10', '2016-06-02 16:01:10');

-- doctor material info
INSERT INTO `doctor_material_infos` (`id`, `farm_id`, `farm_name`, `type`, `material_name`, `remark`, `unit_group_id`, `unit_group_name`, `unit_id`, `unit_name`, `default_consume_count`, `price`, `extra`, `creator_id`, `creator_name`, `updator_id`, `updator_name`, `created_at`, `updated_at`)
VALUES
	(1, 12345, 'farmName', 1, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(2, 12345, 'farmName', 1, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(3, 12345, 'farmName', 1, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(4, 12345, 'farmName', 1, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(5, 12345, 'farmName', 2, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(6, 12345, 'farmName', 2, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(7, 12345, 'farmName', 2, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(8, 12345, 'farmName', 2, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(9, 12345, 'farmName', 3, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(10, 12345, 'farmName', 3, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(11, 12345, 'farmName', 3, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(12, 12345, 'farmName', 3, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(13, 12345, 'farmName', 4, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(14, 12345, 'farmName', 4, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(15, 12345, 'farmName', 4, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(16, 12345, 'farmName', 4, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(17, 12345, 'farmName', 5, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(18, 12345, 'farmName', 5, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(19, 12345, 'farmName', 5, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17'),
	(20, 12345, 'farmName', 5, 'materialName', 'remark', 1, 'unitgroupName', 2, 'unit_name', 100000, 1000, NULL, 1, 'creatorName', 2, 'updatorName', '2016-06-02 16:43:17', '2016-06-02 16:43:17');

--material in ware house info
INSERT INTO `doctor_material_in_ware_houses` (`id`, `farm_id`, `farm_name`, `ware_house_id`, `ware_house_name`, `material_id`, `material_name`, `type`, `lot_number`, `unit_group_name`, `unit_name`, `extra`, `creator_id`, `creator_name`, `updator_id`, `updator_name`, `created_at`, `updated_at`)
VALUES
	(1, 12345, 'farmName', 1, 'warehouseName1', 1, 'materialName1', 1, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(2, 12345, 'farmName', 1, 'warehouseName1', 5, 'materialName2', 1, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(3, 12345, 'farmName', 2, 'warehouseName2', 5, 'materialName3', 1, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(4, 12345, 'farmName', 2, 'warehouseName2', 6, 'materialName4', 1, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(5, 12345, 'farmName', 3, 'warehouseName3', 5, 'materialName5', 2, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(6, 12345, 'farmName', 3, 'warehouseName3', 6, 'materialName6', 2, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(7, 12345, 'farmName', 4, 'warehouseName4', 5, 'materialName7', 2, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(8, 12345, 'farmName', 4, 'warehouseName4', 6, 'materialName8', 2, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(9, 12345, 'farmName', 5, 'warehouseName5', 6, 'materialName9', 3, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(10, 12345, 'farmName', 5, 'warehouseName5', 10, 'materialName10', 3, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(11, 12345, 'farmName', 6, 'warehouseName6', 11, 'materialName11', 3, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(12, 12345, 'farmName', 6, 'warehouseName6', 12, 'materialName12', 3, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(13, 12345, 'farmName', 7, 'warehouseName7', 13, 'materialName13', 4, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(14, 12345, 'farmName', 7, 'warehouseName7', 14, 'materialName14', 4, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(15, 12345, 'farmName', 8, 'warehouseName8', 15, 'materialName15', 4, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(16, 12345, 'farmName', 8, 'warehouseName8', 16, 'materialName16', 4, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(17, 12345, 'farmName', 9, 'warehouseName9', 17, 'materialName17', 5, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(18, 12345, 'farmName', 9, 'warehouseName9', 18, 'materialName18', 5, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(19, 12345, 'farmName', 10, 'warehouseName10', 19, 'materialName19', 5, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32'),
	(20, 12345, 'farmName', 10, 'warehouseName10', 20, 'materialName20', 5, 2500000, 'groupName', 'unitName', NULL, 1, 'creatorName', 1, 'updatorName', '2016-06-02 16:57:32', '2016-06-02 16:57:32');

-- warehouse track info
INSERT INTO `doctor_ware_house_tracks` (`ware_house_id`, `farm_id`, `farm_name`, `manager_id`, `manager_name`, `material_lot_number`, `lot_number`, `is_default`, `extra`, `created_at`, `updated_at`)
VALUES
	(1, 12345, 'farmName', 1, 'managerName', NULL, 5000000, 1, '{"1":2500000,"5":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(2, 12345, 'farmName', 2, 'managerName', NULL, 5000000, 1, '{"5":2500000,"6":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(3, 12345, 'farmName', 3, 'managerName', NULL, 5000000, 2, '{"5":2500000,"6":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(4, 12345, 'farmName', 4, 'managerName', NULL, 5000000, 2, '{"5":2500000,"6":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(5, 12345, 'farmName', 5, 'managerName', NULL, 5000000, 3, '{"6":2500000,"10":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(6, 12345, 'farmName', 1, 'managerName', NULL, 5000000, 3, '{"11":2500000,"12":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(7, 12345, 'farmName', 2, 'managerName', NULL, 5000000, 4, '{"13":2500000,"14":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(8, 12345, 'farmName', 3, 'managerName', NULL, 5000000, 4, '{"15":2500000,"16":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(9, 12345, 'farmName', 4, 'managerName', NULL, 5000000, 5, '{"17":2500000,"18":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53'),
	(10, 12345, 'farmName', 5, 'managerName', NULL, 5000000, 5, '{"19":2500000,"20":2500000}', '2016-05-25 18:43:53', '2016-05-25 18:43:53');

-- basic
INSERT INTO doctor_units VALUES (1, 'unitName', null, now(), now());


-- start 消息相关
INSERT INTO `doctor_message_rule_templates`
(name, type, category, rule_value, status, message_template, content, producer, `describe`, created_at, updated_at, updated_by)
VALUES
-- 系统消息(可配多少小时后)
('系统消息测试', 0, 0, '{"frequence":-1,"channels":"0,1,2,3"}', 1, null, '系统消息测试', 'sysMessageProducer', '系统消息测试', now(), now(), null),
-- id:1 (断奶、流产、返情日期间隔(天))
('待配种母猪提示', 1, 1, '{"values":[{"id":1, "ruleType":1,"value":7, "describe":"断奶、流产、返情日期间隔(天)"}],"frequence":24,"channels":"0,1,2,3"}', 1, 'sow.breed', null, 'sowBreedingProducer', '待配种母猪提示', now(), now(), null),
('待配种母猪警报', 2, 1, '{"values":[{"id":1, "ruleType":1,"value":21, "describe":"断奶、流产、返情日期间隔(天)"}],"frequence":24,"channels":"0,1,2,3"}', 1, 'sow.breed', null, 'sowBreedingProducer', '待配种母猪警报', now(), now(), null),
-- id:1 (母猪已配种时间间隔(天))
('母猪需妊娠检查提示', 1, 2,'{"values":[{"id":1, "ruleType":2,"leftValue":19,"rightValue":25, "describe":"母猪已配种时间间隔(天)"}],"frequence":24,"channels":"0,1,2,3"}',1, 'sow.preg.check', null, 'sowPregCheckProducer', '母猪需妊娠检查提示', now(), now(), null),
-- 母猪需转入妊娠舍提示
('母猪需转入妊娠舍提示', 1, 3,'{"frequence":24,"channels":"0,1,2,3"}',1, 'sow.preg.home', null, 'sowPregHomeProducer', '母猪需转入妊娠舍提示', now(), now(), null),
-- id:1 (预产期提前多少天提醒)
('母猪预产期提示', 1, 4,'{"values":[{"id":1, "ruleType":1,"value":7, "describe":"预产期提前多少天提醒"}],"frequence":24,"channels":"0,1,2,3"}',1, 'sow.birth.date', null, 'sowBirthDateProducer', '母猪预产期提示', now(), now(), null),
-- id:1 (母猪分娩日期起的天数)
('母猪需断奶提示', 1, 5,'{"values":[{"id":1, "ruleType":1,"value":21, "describe":"母猪分娩日期起的天数"}],"frequence":24,"channels":"0,1,2,3"}',
1, 'sow.need.wean', null, 'sowNeedWeanProducer', '母猪需断奶提示', now(), now(), null),
('母猪需断奶警报', 2, 5,'{"values":[{"id":1, "ruleType":1,"value":35, "describe":"母猪分娩日期起的天数"}],"frequence":24,"channels":"0,1,2,3"}',
1, 'sow.need.wean', null, 'sowNeedWeanProducer', '母猪需断奶警报', now(), now(), null),
-- id:1 (母猪胎次)
('母猪应淘汰提示', 1, 6,'{"values":[{"id":1, "ruleType":2, "leftValue":9,"rightValue":10, "describe":"胎次"}],"frequence":24,"channels":"0,1,2,3"}',1, 'sow.eliminate', null, 'sowEliminateProducer', '母猪应淘汰提示', now(), now(), null),
-- id:1 (母猪配种日期起的天数)
('母猪未产仔警报', 2, 10,'{"values":[{"id":1, "ruleType":1,"value":120, "describe":"母猪配种日期起的天数"}],"frequence":24,"channels":"0,1,2,3"}',1, 'sow.not.litter', null, 'sowNotLitterProducer', '母猪未产仔警报', now(), now(), null)
;


INSERT INTO `doctor_messages` (`id`, `farm_id`, `rule_id`, `role_id`, `user_id`, `template_id`, `message_template`, `type`, `category`, `data`, `channel`, `url`, `status`, `sended_at`, `failed_by`, `created_by`, `created_at`, `updated_at`)
VALUES
	(1,NULL,NULL,NULL,1,1,NULL,         0,0,'系统消息测试',                                     0,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(2,NULL,NULL,NULL,2,1,NULL,         0,0,'系统消息测试',                                      0,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(3,1,NULL,NULL,1,2,'template.sow',1,1,'{"pigCode":"2333", "barnName":"猪场1号"}',0,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(4,1,NULL,NULL,1,2,'template.sow',1,1,'{"pigCode":"2333", "barnName":"猪场1号"}',1,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(5,2,NULL,NULL,1,2,'template.sow',1,2,'{"pigCode":"2333", "barnName":"猪场2号"}',0,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(6,2,NULL,NULL,1,2,'template.sow',1,2,'{"pigCode":"2333", "barnName":"猪场2号"}',1,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(7,2,NULL,NULL,1,2,'template.sow',1,2,'{"pigCode":"2333", "barnName":"猪场2号"}',2,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(8,2,NULL,NULL,1,2,'template.sow',1,2,'{"pigCode":"2333", "barnName":"猪场2号"}',3,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(9,3,NULL,NULL,1,2,'template.sow',1,3,'{"pigCode":"2333", "barnName":"猪场3号"}',0,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(10,3,NULL,NULL,1,2,'template.sow',1,3,'{"pigCode":"2333","barnName":"猪场3号"}',1,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(11,3,NULL,NULL,1,2,'template.sow',1,3,'{"pigCode":"2333","barnName":"猪场3号"}',2,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00'),
	(12,4,NULL,NULL,1,2,'template.sow',1,4,'{"pigCode":"2333","barnName":"猪场4号"}',0,'http://m.doctor.com/message/message-detail',1,NULL,NULL,NULL,'2016-06-06 00:00:00','2016-06-06 00:00:00');

-- 消息相关 end