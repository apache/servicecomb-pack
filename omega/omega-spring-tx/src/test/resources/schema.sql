CREATE TABLE IF NOT EXISTS `User` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(36) NOT NULL,
  `email` varchar(36) NOT NULL,
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;
