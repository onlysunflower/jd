CREATE DATABASE IF NOT EXISTS jd_clone_mall
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE jd_clone_mall;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS withdrawal;
DROP TABLE IF EXISTS merchant_settlement;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS refund_log;
DROP TABLE IF EXISTS refund_request;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS order_info;
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS user_coupon;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS product_sku;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS shop;
DROP TABLE IF EXISTS merchant;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  nickname VARCHAR(64),
  phone VARCHAR(32),
  role VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE merchant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  company_name VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64),
  contact_phone VARCHAR(32),
  license_no VARCHAR(64),
  license_image VARCHAR(500),
  commission_rate DECIMAL(5,2) NOT NULL DEFAULT 5.00,
  available_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  frozen_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(32) NOT NULL DEFAULT 'APPROVED',
  reject_reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shop (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(255),
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  sort_order INT DEFAULT 0
);

CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  category_id BIGINT,
  spu_code VARCHAR(64),
  name VARCHAR(128) NOT NULL,
  subtitle VARCHAR(255),
  main_image VARCHAR(500),
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  locked_stock INT NOT NULL DEFAULT 0,
  sales INT NOT NULL DEFAULT 0,
  audit_status VARCHAR(32) NOT NULL,
  shelf_status VARCHAR(32) NOT NULL,
  reject_reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE product_sku (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  sku_code VARCHAR(64) NOT NULL UNIQUE,
  spec_name VARCHAR(128) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  locked_stock INT NOT NULL DEFAULT 0,
  sales INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'OFF',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sku_product (product_id)
);

CREATE TABLE coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  min_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_amount DECIMAL(10,2) NOT NULL,
  total_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ON',
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL
);

CREATE TABLE user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
  locked_order_id BIGINT,
  used_at DATETIME,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_coupon (user_id, status)
);

CREATE TABLE cart_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_id BIGINT,
  quantity INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product_sku (user_id, product_id, sku_id)
);

CREATE TABLE address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  receiver VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  province VARCHAR(64),
  city VARCHAR(64),
  detail VARCHAR(255),
  is_default INT NOT NULL DEFAULT 0
);

CREATE TABLE order_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  coupon_id BIGINT,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  receiver VARCHAR(64),
  receiver_phone VARCHAR(32),
  receiver_address VARCHAR(255),
  logistics_company VARCHAR(64),
  logistics_no VARCHAR(64),
  payment_no VARCHAR(64),
  close_reason VARCHAR(255),
  settlement_status VARCHAR(32) NOT NULL DEFAULT 'UNSETTLED',
  paid_at DATETIME,
  shipped_at DATETIME,
  completed_at DATETIME,
  closed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_id BIGINT,
  sku_code VARCHAR(64),
  spec_name VARCHAR(128),
  product_name VARCHAR(128) NOT NULL,
  product_image VARCHAR(500),
  price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL
);

CREATE TABLE refund_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  source_order_status VARCHAR(32),
  reason VARCHAR(255) NOT NULL,
  evidence_images VARCHAR(1000),
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  merchant_reply VARCHAR(255),
  return_logistics_no VARCHAR(64),
  admin_decision VARCHAR(64),
  admin_remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE refund_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  refund_id BIGINT NOT NULL,
  operator_role VARCHAR(32) NOT NULL,
  operator_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating INT NOT NULL,
  content VARCHAR(500),
  append_content VARCHAR(500),
  reply VARCHAR(500),
  status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
  append_at DATETIME,
  reply_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_product (order_id, product_id)
);

CREATE TABLE merchant_settlement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  merchant_id BIGINT NOT NULL,
  gross_amount DECIMAL(12,2) NOT NULL,
  commission_rate DECIMAL(5,2) NOT NULL,
  commission_amount DECIMAL(12,2) NOT NULL,
  settlement_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  available_at DATETIME NOT NULL,
  refunded_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_settlement_merchant (merchant_id, status)
);

CREATE TABLE withdrawal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  withdraw_no VARCHAR(64) NOT NULL UNIQUE,
  amount DECIMAL(12,2) NOT NULL,
  account_info VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  admin_remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at DATETIME,
  KEY idx_withdrawal_status (status, created_at)
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT,
  operator_role VARCHAR(32),
  module VARCHAR(64),
  action VARCHAR(64),
  detail VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
