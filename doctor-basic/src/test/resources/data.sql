INSERT INTO `doctor_material_price_in_ware_houses` (`id`, `farm_id`, `farm_name`, `ware_house_id`, `ware_house_name`, `material_id`, `material_name`, `type`, `provider_id`, `unit_price`, `remainder`, `provider_time`, `extra`, `creator_id`, `updator_id`, `created_at`, `updated_at`)
VALUES
	(130, 1, '融利实业种猪场', 5, '消耗品仓库', 375, '防疫服', 5, 390, 700, 157.000, '2012-04-11 11:03:15', NULL, NULL, NULL, '2016-10-18 10:27:34', '2016-12-14 19:27:40');


INSERT INTO `doctor_farm_basics` (`id`,`farm_id`,`basic_ids`,`reason_ids`,`material_ids`,`extra`,`created_at`,`updated_at`) VALUES (168,407,'1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,116,117,118,119,120,121,122,123,124,133,134,135,138,144,152,153,154,156,157,167,173,174,177,204,223,227,228,229,230,231,232,233,234,235,237,238,239,243','17,19,20,21,27,28,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,309,310,311,312,313,314,316,317,318,319,320,321,322,323,324,325,326,327,328,329,330,331','1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,396,397,398,466,467,468,469,470,471,472,473,474,475,488,489,490,491,492,493,494,495,496,497,498,499,500,501,502,503,504,505,506,507,508,509,510,511,512,513,514,515,516,517,518,519,520,521,522,523,524,525,526,527,528,529,530,531,532,533,534,535,536,537,538,539,540,541,542,543,544,545,546,547,548,549,550,551,552,553,554,555,556,557,558,559,560,561,562,563,564,565,566,567,568,569,570,127,133,134,135,138,144,405,442,443,444,445,446,447,448,449,456,457,458,459,460,461,462,463,465,152,153,154,156,157,167,173,174,177,204,223,402,403,404,420,421,422,423,424,425,426,427,428,429,430,431,432,433,434,435,440,453,454,455,476,477,478,483,485,227,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,309,310,311,312,313,314,315,316,317,318,319,320,321,322,323,324,325,326,327,328,329,330,331,332,333,334,335,336,337,338,339,340,341,342,343,344,345,346,347,348,349,350,351,352,353,354,355,356,357,358,359,360,361,362,363,364,365,366,367,368,369,370,371,372,373,374,375,376,377,378,379,380,381,382,383,384,385,386,387,388,389,390,391,392,393,394',NULL,'2017-04-01 17:08:54','2017-07-18 15:31:55');
INSERT INTO `doctor_basic_materials` (`id`,`type`,`sub_type`,`name`,`srm`,`is_valid`,`unit_group_id`,`unit_group_name`,`unit_id`,`unit_name`,`default_consume_count`,`price`,`remark`,`created_at`,`updated_at`) VALUES (153,2,NULL,'正大12%哺乳料','zd12%brl',1,NULL,'千克',NULL,'千克',NULL,NULL,'','2016-10-18 10:27:30','2016-10-18 10:27:30');
INSERT INTO `doctor_basic_materials` (`id`,`type`,`sub_type`,`name`,`srm`,`is_valid`,`unit_group_id`,`unit_group_name`,`unit_id`,`unit_name`,`default_consume_count`,`price`,`remark`,`created_at`,`updated_at`) VALUES (157,2,NULL,'玉米','ym',1,NULL,'千克',NULL,'千克',NULL,NULL,'','2016-10-18 10:27:30','2016-10-18 10:27:30');
INSERT INTO `doctor_basic_materials` (`id`,`type`,`sub_type`,`name`,`srm`,`is_valid`,`unit_group_id`,`unit_group_name`,`unit_id`,`unit_name`,`default_consume_count`,`price`,`remark`,`created_at`,`updated_at`) VALUES (167,2,NULL,'麦麸','mf1',1,NULL,'千克',NULL,'千克',NULL,NULL,'','2016-10-18 10:27:30','2016-10-18 10:27:30');
INSERT INTO `doctor_basic_materials` (`id`,`type`,`sub_type`,`name`,`srm`,`is_valid`,`unit_group_id`,`unit_group_name`,`unit_id`,`unit_name`,`default_consume_count`,`price`,`remark`,`created_at`,`updated_at`) VALUES (227,1,NULL,'正大后备料','zdhbl',1,NULL,'千克',NULL,'千克',NULL,NULL,'','2016-10-18 10:27:30','2016-10-18 10:27:30');
INSERT INTO `doctor_basic_materials` (`id`,`type`,`sub_type`,`name`,`srm`,`is_valid`,`unit_group_id`,`unit_group_name`,`unit_id`,`unit_name`,`default_consume_count`,`price`,`remark`,`created_at`,`updated_at`) VALUES (228,1,NULL,'正大哺乳料','zdbrl',1,NULL,'千克',NULL,'千克',NULL,NULL,'','2016-10-18 10:27:30','2016-10-18 10:27:30');

INSERT INTO `doctor_ware_houses` (`id`,`ware_house_name`,`farm_id`,`farm_name`,`manager_id`,`manager_name`,`address`,`type`,`extra`,`creator_id`,`creator_name`,`updator_id`,`updator_name`,`created_at`,`updated_at`) VALUES (177,'饲料仓库',407,'演示猪场八区',12220,'张三','',1,NULL,12219,'yszc8q',NULL,NULL,'2017-05-08 11:25:09','2017-05-08 11:25:09');
INSERT INTO `doctor_ware_houses` (`id`,`ware_house_name`,`farm_id`,`farm_name`,`manager_id`,`manager_name`,`address`,`type`,`extra`,`creator_id`,`creator_name`,`updator_id`,`updator_name`,`created_at`,`updated_at`) VALUES (189,'药品仓库',407,'演示猪场八区',12030,'李咏','',4,NULL,91,'lyy',NULL,NULL,'2017-05-15 11:49:19','2017-05-15 11:49:19');
INSERT INTO `doctor_ware_houses` (`id`,`ware_house_name`,`farm_id`,`farm_name`,`manager_id`,`manager_name`,`address`,`type`,`extra`,`creator_id`,`creator_name`,`updator_id`,`updator_name`,`created_at`,`updated_at`) VALUES (190,'原料仓库',407,'演示猪场八区',12030,'李咏','',2,NULL,91,'lyy',NULL,NULL,'2017-05-15 11:49:42','2017-05-15 11:49:42');
INSERT INTO `doctor_ware_houses` (`id`,`ware_house_name`,`farm_id`,`farm_name`,`manager_id`,`manager_name`,`address`,`type`,`extra`,`creator_id`,`creator_name`,`updator_id`,`updator_name`,`created_at`,`updated_at`) VALUES (200,'仓库11',407,'演示猪场八区',1,NULL,NULL,3,NULL,10390,'lxzc',NULL,NULL,'2017-08-23 19:17:40','2017-08-23 19:17:40');
INSERT INTO `doctor_ware_houses` (`id`,`ware_house_name`,`farm_id`,`farm_name`,`manager_id`,`manager_name`,`address`,`type`,`extra`,`creator_id`,`creator_name`,`updator_id`,`updator_name`,`created_at`,`updated_at`) VALUES (201,'原料仓库2',407,'演示猪场八区',12030,'李咏',NULL,2,NULL,91,'lyy',NULL,NULL,'2017-08-23 19:17:40','2017-08-23 19:17:40');


insert into `doctor_warehouse_stock`(`id`,`warehouse_id`,`warehouse_name`,`warehouse_type`,`farm_id`,`sku_id`,`sku_name`,`quantity`,`created_at`,`updated_at`)values(1,1,'仓库',1,1,1,'sku',10,'2017-08-23 19:17:40','2017-08-23 19:17:40');
insert into `doctor_warehouse_stock`(`id`,`warehouse_id`,`warehouse_name`,`warehouse_type`,`farm_id`,`sku_id`,`sku_name`,`quantity`,`created_at`,`updated_at`)values(2,1,'仓库',1,1,2,'sku2',0,'2017-08-23 19:17:40','2017-08-23 19:17:40');