/*
 Navicat Premium Data Transfer

 Source Server         : sukuncan
 Source Server Type    : MySQL
 Source Server Version : 50742
 Source Host           : localhost:3306
 Source Schema         : xiaosudb

 Target Server Type    : MySQL
 Target Server Version : 50742
 File Encoding         : 65001

 Date: 19/06/2025 00:10:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for students
-- ----------------------------
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of students
-- ----------------------------
INSERT INTO `students` VALUES (1, '123456', '123456', 1);
INSERT INTO `students` VALUES (2, 'hongshayin', '123456', 0);
INSERT INTO `students` VALUES (3, 'sukuncan', '123456', 0);
INSERT INTO `students` VALUES (4, 'zyx', '123456', 0);
INSERT INTO `students` VALUES (5, 'zhang', '123456', 1);
INSERT INTO `students` VALUES (6, 'sukun', '123456', 1);
INSERT INTO `students` VALUES (7, 'ying', '123456', 0);
INSERT INTO `students` VALUES (8, 'zhangyingxuan', '123456', 0);
INSERT INTO `students` VALUES (9, 'susu', '123456', 0);
INSERT INTO `students` VALUES (10, 'sususu', '123456', 0);

SET FOREIGN_KEY_CHECKS = 1;
