alter table parana_user_profiles
modify column user_id  bigint(20) NOT NULL COMMENT '用户id',
modify column `province_id` bigint(20) NULL COMMENT '省id',
modify column `province` VARCHAR(100) NULL COMMENT '省',
modify column `avatar` VARCHAR(512) NULL COMMENT '头像';

alter table doctor_service_reviews
add column `real_name` VARCHAR (16) DEFAULT NULL COMMENT '用户申请服务时填写的真实姓名' after user_mobile;

-- 消息规则模板初始化
alter table parana_message_templates
modify column content text NOT NULL COMMENT '消息的内容模板, handlebars格式';

INSERT INTO `doctor_message_rule_templates`
(name, type, category, rule_value, status, message_template, content, producer, `describe`, created_at, updated_at, updated_by)
VALUES
('待配种提醒', 1, 1, '{"values":[{"id":1, "ruleType":1,"value":7, "describe":"断奶、流产、返情日期间隔(天)"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}', 1, 'msg.sow.breed', null, 'sowBreedingProducer', '待配种母猪提示', now(), now(), null),
('待配种警示', 2, 1,'{"values":[{"id":1, "ruleType":1,"value":21, "describe":"断奶、流产、返情日期间隔(天)"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}', 1, 'msg.sow.breed', null, 'sowBreedingProducer', '待配种母猪警报', now(), now(), null),
('妊娠检查提醒', 1, 2,'{"values":[{"id":1, "ruleType":2,"leftValue":19,"rightValue":25, "describe":"母猪已配种时间间隔(天)"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.sow.preg.check', null, 'sowPregCheckProducer', '母猪需妊娠检查提示', now(), now(), null),
('妊娠转入提醒', 1, 3,'{"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.sow.preg.home', null, 'sowPregHomeProducer', '母猪需转入妊娠舍提示', now(), now(), null),
('预产提醒', 1, 4,'{"values":[{"id":1, "ruleType":1,"value":7, "describe":"预产期提前多少天提醒"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.sow.birth.date', null, 'sowBirthDateProducer', '母猪预产期提示', now(), now(), null),
('断奶提醒', 1, 5,'{"values":[{"id":1, "ruleType":1,"value":21, "describe":"母猪分娩日期起的天数"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.sow.need.wean', null, 'sowNeedWeanProducer', '母猪需断奶提示', now(), now(), null),
('断奶警示', 2, 5,'{"values":[{"id":1, "ruleType":1,"value":35, "describe":"母猪分娩日期起的天数"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.sow.need.wean', null, 'sowNeedWeanProducer', '母猪需断奶警报', now(), now(), null),
('母猪淘汰提醒', 1, 6,'{"values":[{"id":1, "ruleType":1, "value":10, "describe":"胎次"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.sow.eliminate', null, 'sowEliminateProducer', '母猪应淘汰提示', now(), now(), null),
('公猪淘汰提醒', 1, 7,'{"values":[{"id":1, "ruleType":1, "value":20, "describe":"公猪配种次数"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.boar.eliminate', null, 'boarEliminateProducer', '公猪应淘汰提示', now(), now(), null),
('免疫提醒', 1, 8,'{"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.pig.vaccination', null, 'pigVaccinationProducer', '猪只免疫提示', now(), now(), null),
('产仔警示', 2, 10,'{"values":[{"id":1, "ruleType":1,"value":120, "describe":"母猪配种日期起的天数"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.sow.not.litter', null, 'sowNotLitterProducer', '母猪未产仔警报', now(), now(), null),
('库存提醒', 1, 9,'{"values":[{"id":1, "ruleType":1,"value":7, "describe":"库存量"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.warehouse.store', null, 'storageShortageProducer', '仓库库存不足提示', now(), now(), null),
('库存警示', 2, 9,'{"values":[{"id":1, "ruleType":1,"value":3, "describe":"库存量"}],"frequence":24,"channels":"0,1,2,3","url":"/message/detail"}',1, 'msg.warehouse.store', null, 'storageShortageProducer', '仓库库存不足警报', now(), now(), null);

-- 发送消息模板
INSERT INTO `parana_message_templates` (`creator_id`, `creator_name`, `name`, `title`, `content`, `context`, `channel`, `disabled`, `description`, `created_at`, `updated_at`)
VALUES
(1, 'admin', 'msg.sys.normal.sys',   '一般系统消息', '{{data}}', '{"data":"系统消息提示"}', 0, 0, '一般系统消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sys.normal.sms', 	 '一般系统消息', '{{data}}', '{"data":"系统消息提示"}', 1, 0, '一般系统消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sys.normal.email', '一般系统消息', '{{data}}', '{"data":"系统消息提示"}', 2, 0, '一般系统消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sys.normal.app',   '一般系统消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{data}}","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{data}}"}}}', '{"data":"系统消息提示"}', 3, 0, '一般系统消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.breed.sys',   '母猪待配种消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行配种。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '母猪待配种消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.breed.sms',   '母猪待配种消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行配种。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '母猪待配种消息短息模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.breed.email', '母猪待配种消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行配种。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '母猪待配种消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.breed.app',   '母猪待配种消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行配种。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行配种。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '母猪待配种消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.check.sys',   '母猪妊娠检查消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行妊娠检查。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '母猪妊娠检查消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.check.sms',   '母猪妊娠检查消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行妊娠检查。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '母猪妊娠检查消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.check.email', '母猪妊娠检查消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行妊娠检查。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '母猪妊娠检查消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.check.app',   '母猪妊娠检查消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行妊娠检查。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时进行妊娠检查。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '母猪妊娠检查消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.home.sys',   '母猪转舍消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 且尚未转舍, 应及时转舍。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '母猪转舍消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.home.sms',   '母猪转舍消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 且尚未转舍, 应及时转舍。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '母猪转舍消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.home.email', '母猪转舍消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 且尚未转舍, 应及时转舍。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '母猪转舍消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.preg.home.app',   '母猪转舍消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 且尚未转舍, 应及时转舍。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 且尚未转舍, 应及时转舍。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '母猪转舍消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.birth.date.sys',   '母猪预产消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 即将抵达预产期时间, 预产期为{{judgePregDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '母猪预产消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.birth.date.sms',   '母猪预产消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 即将抵达预产期时间, 预产期为{{judgePregDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '母猪预产消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.birth.date.email', '母猪预产消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 即将抵达预产期时间, 预产期为{{judgePregDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '母猪预产消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.birth.date.app',   '母猪预产消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 即将抵达预产期时间, 预产期为{{judgePregDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 即将抵达预产期时间, 预产期为{{judgePregDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '母猪预产消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.need.wean.sys',   '母猪断奶消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时断奶。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '母猪断奶消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.need.wean.sms',   '母猪断奶消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时断奶。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '母猪断奶消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.need.wean.email', '母猪断奶消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时断奶。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '母猪断奶消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.need.wean.app',   '母猪断奶消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时断奶。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 应及时断奶。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '母猪断奶消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.eliminate.sys',   '母猪淘汰消息', '{{pigCode}}母猪的胎次已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '母猪淘汰消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.eliminate.sms',   '母猪淘汰消息', '{{pigCode}}母猪的胎次已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '母猪淘汰消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.eliminate.email', '母猪淘汰消息', '{{pigCode}}母猪的胎次已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '母猪淘汰消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.eliminate.app',   '母猪淘汰消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}母猪的胎次已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}母猪的胎次已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '母猪淘汰消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.boar.eliminate.sys',   '公猪淘汰消息', '{{pigCode}}公猪的配种次数已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '公猪淘汰消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.boar.eliminate.sms',   '公猪淘汰消息', '{{pigCode}}公猪的配种次数已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '公猪淘汰消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.boar.eliminate.email', '公猪淘汰消息', '{{pigCode}}公猪的配种次数已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '公猪淘汰消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.boar.eliminate.app',   '公猪淘汰消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}公猪的配种次数已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}公猪的配种次数已达{{parity}}, 应需被淘汰。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '公猪淘汰消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.not.litter.sys',   '母猪未产仔消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 还未产仔, 配种日期为{{matingDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 0, 0, '母猪未产仔消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.not.litter.sms',   '母猪未产仔消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 还未产仔, 配种日期为{{matingDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 1, 0, '母猪未产仔消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.not.litter.email', '母猪未产仔消息', '{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 还未产仔, 配种日期为{{matingDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。', '', 2, 0, '母猪未产仔消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.sow.not.litter.app',   '母猪未产仔消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 还未产仔, 配种日期为{{matingDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{pigCode}}母猪处于{{statusName}}状态已经{{timeDiff}}天了, 还未产仔, 配种日期为{{matingDate}}。猪场为{{farmName}}, 猪舍为{{barnName}}。"}}}', '', 3, 0, '母猪未产仔消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.warehouse.store.sys',   '仓库库存不足消息', '{{wareHouseName}}仓库的{{materialName}}原料已经不足{{lotConsumeDay}}天, 剩余量为{{lotNumber}}。猪场为{{farmName}}。', '', 0, 0, '仓库库存不足消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.warehouse.store.sms',   '仓库库存不足消息', '{{wareHouseName}}仓库的{{materialName}}原料已经不足{{lotConsumeDay}}天, 剩余量为{{lotNumber}}。猪场为{{farmName}}。', '', 1, 0, '仓库库存不足消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.warehouse.store.email', '仓库库存不足消息', '{{wareHouseName}}仓库的{{materialName}}原料已经不足{{lotConsumeDay}}天, 剩余量为{{lotNumber}}。猪场为{{farmName}}。', '', 2, 0, '仓库库存不足消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.warehouse.store.app',   '仓库库存不足消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{wareHouseName}}仓库的{{materialName}}原料已经不足{{lotConsumeDay}}天, 剩余量为{{lotNumber}}。猪场为{{farmName}}。","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{wareHouseName}}仓库的{{materialName}}原料已经不足{{lotConsumeDay}}天, 剩余量为{{lotNumber}}。猪场为{{farmName}}。"}}}', '', 3, 0, '仓库库存不足消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.pig.vaccination.sys',   '猪只免疫消息', '{{#of pigType "4,5,6,7,8,9"}}{{#of vaccinationDateType "1"}}{{pigCode}}猪只日龄已经超过{{inputValue}}天, 当前日龄为{{dateAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{pigCode}}猪只应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{pigCode}}猪只体重已经超过{{inputValue}}kg, 当前体重为{{weight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "4"}}{{pigCode}}猪只转舍后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "6"}}{{pigCode}}猪只妊娠检查后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "7"}}{{pigCode}}猪只配种后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "8"}}{{pigCode}}猪只分娩后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "9"}}{{pigCode}}猪只断奶后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}{{#of pigType "1,2,3"}}{{#of vaccinationDateType "1"}}{{groupCode}}猪群平均日龄已经超过{{inputValue}}天, 当前日龄为{{avgDayAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{groupCode}}猪群应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{groupCode}}猪群平均体重已经超过{{inputValue}}kg, 当前体重为{{avgWeight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "5"}}{{groupCode}}猪群转群后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}', '', 0, 0, '猪只免疫消息消息站内模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.pig.vaccination.sms',   '猪只免疫消息', '{{#of pigType "4,5,6,7,8,9"}}{{#of vaccinationDateType "1"}}{{pigCode}}猪只日龄已经超过{{inputValue}}天, 当前日龄为{{dateAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{pigCode}}猪只应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{pigCode}}猪只体重已经超过{{inputValue}}kg, 当前体重为{{weight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "4"}}{{pigCode}}猪只转舍后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "6"}}{{pigCode}}猪只妊娠检查后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "7"}}{{pigCode}}猪只配种后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "8"}}{{pigCode}}猪只分娩后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "9"}}{{pigCode}}猪只断奶后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}{{#of pigType "1,2,3"}}{{#of vaccinationDateType "1"}}{{groupCode}}猪群平均日龄已经超过{{inputValue}}天, 当前日龄为{{avgDayAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{groupCode}}猪群应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{groupCode}}猪群平均体重已经超过{{inputValue}}kg, 当前体重为{{avgWeight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "5"}}{{groupCode}}猪群转群后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}', '', 1, 0, '猪只免疫消息消息短信模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.pig.vaccination.email', '猪只免疫消息', '{{#of pigType "4,5,6,7,8,9"}}{{#of vaccinationDateType "1"}}{{pigCode}}猪只日龄已经超过{{inputValue}}天, 当前日龄为{{dateAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{pigCode}}猪只应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{pigCode}}猪只体重已经超过{{inputValue}}kg, 当前体重为{{weight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "4"}}{{pigCode}}猪只转舍后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "6"}}{{pigCode}}猪只妊娠检查后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "7"}}{{pigCode}}猪只配种后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "8"}}{{pigCode}}猪只分娩后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "9"}}{{pigCode}}猪只断奶后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}{{#of pigType "1,2,3"}}{{#of vaccinationDateType "1"}}{{groupCode}}猪群平均日龄已经超过{{inputValue}}天, 当前日龄为{{avgDayAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{groupCode}}猪群应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{groupCode}}猪群平均体重已经超过{{inputValue}}kg, 当前体重为{{avgWeight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "5"}}{{groupCode}}猪群转群后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}', '', 2, 0, '猪只免疫消息消息邮件模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43'),
(1, 'admin', 'msg.pig.vaccination.app',   '猪只免疫消息', '{"payload":{"body":{"ticker":"{{ticker}}","title":"{{title}}","text":"{{#of pigType "4,5,6,7,8,9"}}{{#of vaccinationDateType "1"}}{{pigCode}}猪只日龄已经超过{{inputValue}}天, 当前日龄为{{dateAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{pigCode}}猪只应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{pigCode}}猪只体重已经超过{{inputValue}}kg, 当前体重为{{weight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "4"}}{{pigCode}}猪只转舍后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "6"}}{{pigCode}}猪只妊娠检查后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "7"}}{{pigCode}}猪只配种后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "8"}}{{pigCode}}猪只分娩后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "9"}}{{pigCode}}猪只断奶后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}{{#of pigType "1,2,3"}}{{#of vaccinationDateType "1"}}{{groupCode}}猪群平均日龄已经超过{{inputValue}}天, 当前日龄为{{avgDayAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{groupCode}}猪群应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{groupCode}}猪群平均体重已经超过{{inputValue}}kg, 当前体重为{{avgWeight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "5"}}{{groupCode}}猪群转群后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}","after_open":"{{after_open}}", "url":"{{{url}}}"}, "aps":{"alert":"{{title}}\\n{{#of pigType "4,5,6,7,8,9"}}{{#of vaccinationDateType "1"}}{{pigCode}}猪只日龄已经超过{{inputValue}}天, 当前日龄为{{dateAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{pigCode}}猪只应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{pigCode}}猪只体重已经超过{{inputValue}}kg, 当前体重为{{weight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "4"}}{{pigCode}}猪只转舍后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "6"}}{{pigCode}}猪只妊娠检查后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "7"}}{{pigCode}}猪只配种后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "8"}}{{pigCode}}猪只分娩后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "9"}}{{pigCode}}猪只断奶后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}{{#of pigType "1,2,3"}}{{#of vaccinationDateType "1"}}{{groupCode}}猪群平均日龄已经超过{{inputValue}}天, 当前日龄为{{avgDayAge}}, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "2"}}当前日期已经超过{{inputDate}}, {{groupCode}}猪群应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}} 猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "3"}}{{groupCode}}猪群平均体重已经超过{{inputValue}}kg, 当前体重为{{avgWeight}}kg, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{#of vaccinationDateType "5"}}{{groupCode}}猪群转群后已经超过{{inputValue}}天, 应及时免疫。使用疫苗{{materialName}}, 使用量为{{dose}}。{{#if vaccDate}}上次免疫时间为:{{vaccDate}}, {{/if}}猪场为{{farmName}}, 猪舍为{{barnName}}。{{/of}}{{/of}}"}}}', '', 3, 0, '猪只免疫消息消息推送模板', '2016-06-15 17:08:43', '2016-06-15 17:08:43');

-- 2016-06-24 增加输入码, 增加基础数据表
ALTER TABLE doctor_diseases ADD COLUMN `srm` VARCHAR (32) DEFAULT NULL COMMENT '输入码(快捷输入)' AFTER farm_name;

-- 新增基础数据表, 整合一些基础数据
DROP TABLE IF EXISTS `doctor_basics`;
CREATE TABLE `doctor_basics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `name` varchar(32) DEFAULT NULL COMMENT '基础数据内容',
  `type` smallint(6) DEFAULT NULL COMMENT '基础数据类型 枚举',
  `type_name` varchar(32) DEFAULT NULL COMMENT '数据类型名称',
  `srm` varchar(32) DEFAULT NULL COMMENT '输入码(快捷输入用)',
  `out_id` varchar(128) DEFAULT NULL COMMENT '外部id',
  `extra` text COMMENT '附加字段',
  `updator_id` bigint(20) DEFAULT NULL COMMENT '更新人id',
  `updator_name` varchar(64) DEFAULT NULL COMMENT '更新人name',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='基础数据表';

ALTER TABLE doctor_basics ADD COLUMN `is_valid` smallint(6) DEFAULT NULL COMMENT '逻辑删除字段, -1 表示删除' AFTER type_name;
ALTER TABLE doctor_basics ADD COLUMN `context` VARCHAR(64) DEFAULT NULL COMMENT '基础数据内容' AFTER srm;

ALTER TABLE doctor_change_reasons ADD COLUMN `srm` VARCHAR(20) DEFAULT NULL COMMENT 'reason字段的输入码' AFTER reason;

-- 2016-06-28 doctor_pig_tracks 表增加 pig_type 猪类冗余字段
ALTER TABLE doctor_pig_tracks ADD COLUMN `pig_type` smallint(6) DEFAULT NULL COMMENT '猪类型(公猪，母猪， 仔猪)' AFTER pig_id;

-- 2016-07-04 groupEvent表增加更新信息字段
ALTER TABLE doctor_group_events ADD COLUMN `updated_at` datetime DEFAULT NULL COMMENT '更新时间' AFTER creator_name;
ALTER TABLE doctor_group_events ADD COLUMN `updator_id` bigint(20) DEFAULT NULL COMMENT '更新人id' AFTER updated_at;
ALTER TABLE doctor_group_events ADD COLUMN `updator_name` varchar(64) DEFAULT NULL COMMENT '更新人name' AFTER updator_id;

alter table doctor_user_subs
add column `real_name` VARCHAR(64) DEFAULT NULL COMMENT '真实姓名 (冗余),跟随 user_profile 表的 real_name 字段' after `user_name`;

update doctor_user_subs o
set o.real_name = (select i.realname from parana_user_profiles i where i.user_id = o.user_id);

-- 20160-07-06 doctor_pig_track 表增加消息提醒字段
ALTER TABLE doctor_pig_tracks
ADD COLUMN `extra_message` text DEFAULT NULL COMMENT '每只猪的消息提醒' AFTER `extra`;

-- 2016-07-16 新建基础物料表
DROP TABLE IF EXISTS `doctor_basic_materials`;
CREATE TABLE `doctor_basic_materials` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `type` smallint(6) DEFAULT NULL COMMENT '物料类型',
  `name` varchar(128) DEFAULT NULL COMMENT '物料名称',
  `srm` varchar(32) DEFAULT NULL,
  `unit_group_id` bigint(20) unsigned DEFAULT NULL COMMENT '单位组id',
  `unit_group_name` varchar(64) DEFAULT NULL COMMENT '单位组名称',
  `unit_id` bigint(20) unsigned DEFAULT NULL COMMENT '单位id',
  `unit_name` varchar(64) DEFAULT NULL COMMENT '单位名称',
  `default_consume_count` int(11) DEFAULT NULL COMMENT '默认消耗数量',
  `price` bigint(20) DEFAULT NULL COMMENT '价格(元)',
  `remark` text COMMENT '标注',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='基础物料表';

-- 2016-07-19 猪场日报表
CREATE TABLE `doctor_daily_reports` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `farm_id` bigint(20) DEFAULT NULL COMMENT '猪场id',
  `farm_name` varchar(64) DEFAULT NULL COMMENT '猪场名称',
  `data` text COMMENT '日报数据，json存储',
  `extra` text COMMENT '附加字段',
  `sum_at` date DEFAULT NULL COMMENT '统计时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间(仅做记录创建时间，不参与查询)',
  PRIMARY KEY (`id`),
  KEY `idx_doctor_daily_reports_farm_id_agg_sumat` (`farm_id`,`sum_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='猪场日报表';

-- 数据迁移的数据源信息
CREATE TABLE `doctor_move_datasource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `name` varchar(128) DEFAULT NULL COMMENT '数据源名称',
  `username` varchar(32) DEFAULT NULL COMMENT '数据库用户名',
  `password` varchar(32) DEFAULT NULL COMMENT '数据库密码',
  `driver` varchar(32) DEFAULT NULL COMMENT 'jdbc driver',
  `url` varchar(128) DEFAULT NULL COMMENT '链接url',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='move-data数据源信息';

-- 添加灵宝融利的数据源信息
INSERT INTO `doctor_move_datasource` (`id`, `name`, `username`, `password`, `driver`, `url`)
VALUES
	(1, '灵宝融利', 'sa', 'pigmall', 'net.sourceforge.jtds.jdbc.Driver', 'jdbc:jtds:sqlserver://101.201.146.171:1433/lbrl;tds=8.0;lastupdatecount=true');
