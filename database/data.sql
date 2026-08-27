USE jd_clone_mall;

INSERT INTO user (id, username, password, nickname, phone, role, status) VALUES
(1, 'user001', '123456', '演示用户', '13800000001', 'USER', 'NORMAL'),
(2, 'merchant001', '123456', '京东自营商家', '13800000002', 'MERCHANT', 'NORMAL'),
(3, 'service_admin', '123456', '客服管理员', '13800000003', 'SERVICE_ADMIN', 'NORMAL'),
(4, 'product_admin', '123456', '商品审核员', '13800000004', 'PRODUCT_ADMIN', 'NORMAL'),
(5, 'super_admin', '123456', '超级管理员', '13800000005', 'SUPER_ADMIN', 'NORMAL'),
(6, 'user002', '123456', '测试买家二号', '13800000006', 'USER', 'NORMAL'),
(7, 'merchant002', '123456', '数码配件商家', '13800000007', 'MERCHANT', 'NORMAL');

INSERT INTO merchant (id, user_id, company_name, contact_name, contact_phone, status, reject_reason) VALUES
(1, 2, '京东课程项目示例商家', '张三', '13800000002', 'APPROVED', NULL),
(2, 7, '晨星数码配件有限公司', '李四', '13800000007', 'APPROVED', NULL),
(3, 5, '待审核家电旗舰店', '王五', '13800000008', 'PENDING', '资质材料待补充');

INSERT INTO shop (id, merchant_id, name, description, status) VALUES
(1, 1, '京东自营演示店', '用于课程答辩演示的商家店铺', 'NORMAL'),
(2, 2, '晨星数码配件店', '用于测试多商家商品和订单', 'NORMAL'),
(3, 3, '待审核家电旗舰店', '商家入驻审核演示店铺', 'PENDING');

INSERT INTO category (id, name, parent_id, sort_order) VALUES
(1, '手机数码', 0, 1),
(2, '电脑办公', 0, 2),
(3, '家用电器', 0, 3),
(4, '生活百货', 0, 4),
(5, '生鲜食品', 0, 5);

INSERT INTO product
(id, merchant_id, category_id, name, subtitle, main_image, price, stock, sales, audit_status, shelf_status, reject_reason)
VALUES
(1, 1, 1, 'JDPhone Pro 15', '高刷屏幕，长续航，适合演示下单流程', '/products/phone.png', 4999.00, 78, 12, 'APPROVED', 'ON', NULL),
(2, 1, 2, 'JDBook Air 14', '轻薄办公本，适合学生课程项目展示', '/products/laptop.png', 5999.00, 44, 8, 'APPROVED', 'ON', NULL),
(3, 1, 3, 'JD Smart TV 65', '大屏电视，模拟家电类商品', '/products/laptop.png', 3299.00, 29, 16, 'APPROVED', 'ON', NULL),
(4, 1, 1, '待审核无线耳机', '商家新提交，等待管理员审核', 'https://dummyimage.com/600x450/f59e0b/ffffff&text=Pending+Earbuds', 399.00, 120, 0, 'PENDING', 'OFF', NULL),
(5, 1, 4, '京造恒温电热水壶', '生活百货类商品，用于测试分类筛选', '/products/kettle.png', 129.00, 160, 33, 'APPROVED', 'ON', NULL),
(6, 1, 5, '陕西红富士苹果礼盒', '生鲜商品，测试低价和高库存场景', 'https://dummyimage.com/600x450/d97706/ffffff&text=Fresh+Apple', 59.90, 260, 82, 'APPROVED', 'ON', NULL),
(7, 2, 1, 'Type-C 快充数据线 1.5m', '配件商家商品，适合测试多商家数据', 'https://dummyimage.com/600x450/7c3aed/ffffff&text=USB-C+Cable', 29.90, 480, 128, 'APPROVED', 'ON', NULL),
(8, 2, 1, '主动降噪蓝牙耳机 Max', '数码配件爆款商品，库存较低', '/products/headphones.png', 699.00, 9, 67, 'APPROVED', 'ON', NULL),
(9, 2, 2, '机械键盘 K87', '办公外设商品，支持后续扩展评价', 'https://dummyimage.com/600x450/475569/ffffff&text=Keyboard+K87', 259.00, 70, 45, 'APPROVED', 'ON', NULL),
(10, 2, 4, '人体工学升降支架', '办公桌面改善用品', 'https://dummyimage.com/600x450/0f766e/ffffff&text=Laptop+Stand', 169.00, 35, 21, 'APPROVED', 'ON', NULL),
(11, 1, 3, '待审核空气净化器', '管理员后台可审核通过或驳回', 'https://dummyimage.com/600x450/64748b/ffffff&text=Pending+Air', 1299.00, 66, 0, 'PENDING', 'OFF', NULL),
(12, 2, 1, '被驳回智能手表', '商品图片和参数不完整，等待商家修改', 'https://dummyimage.com/600x450/ef4444/ffffff&text=Rejected+Watch', 899.00, 42, 0, 'REJECTED', 'OFF', '参数描述不完整');

INSERT INTO address (id, user_id, receiver, phone, province, city, detail, is_default) VALUES
(1, 1, '演示用户', '13800000001', '北京', '北京市', '朝阳区京东课程项目演示地址', 1),
(2, 1, '演示用户', '13800000001', '上海', '上海市', '浦东新区测试收货地址', 0),
(3, 6, '测试买家二号', '13800000006', '广东', '深圳市', '南山区测试地址', 1);

INSERT INTO order_info
(id, order_no, user_id, merchant_id, total_amount, status, receiver, receiver_phone, receiver_address, logistics_company, logistics_no, paid_at, shipped_at, completed_at, created_at)
VALUES
(1, 'JD202608230001', 1, 1, 4999.00, 'WAIT_PAY', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', NULL, NULL, NULL, NULL, NULL, '2026-08-23 09:10:00'),
(2, 'JD202608230002', 1, 1, 5999.00, 'WAIT_SHIP', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', NULL, NULL, '2026-08-23 09:18:00', NULL, NULL, '2026-08-23 09:16:00'),
(3, 'JD202608230003', 1, 1, 3299.00, 'WAIT_RECEIVE', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', '京东快递', 'JDV0003001', '2026-08-22 10:00:00', '2026-08-22 14:20:00', NULL, '2026-08-22 09:58:00'),
(4, 'JD202608230004', 1, 2, 699.00, 'COMPLETED', '演示用户', '13800000001', '上海市浦东新区测试收货地址', '京东快递', 'JDV0004001', '2026-08-21 11:00:00', '2026-08-21 16:20:00', '2026-08-22 18:30:00', '2026-08-21 10:55:00'),
(5, 'JD202608230005', 1, 2, 259.00, 'REFUNDING', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', '京东快递', 'JDV0005001', '2026-08-20 12:00:00', '2026-08-20 18:15:00', NULL, '2026-08-20 11:50:00'),
(6, 'JD202608230006', 1, 1, 129.00, 'REFUNDING', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', '京东快递', 'JDV0006001', '2026-08-19 15:00:00', '2026-08-19 19:00:00', NULL, '2026-08-19 14:58:00'),
(7, 'JD202608230007', 1, 1, 59.90, 'REFUNDED', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', '京东快递', 'JDV0007001', '2026-08-18 10:00:00', '2026-08-18 15:30:00', NULL, '2026-08-18 09:40:00'),
(8, 'JD202608230008', 1, 2, 29.90, 'CANCELED', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', NULL, NULL, NULL, NULL, NULL, '2026-08-17 20:30:00'),
(9, 'JD202608230009', 6, 2, 169.00, 'WAIT_SHIP', '测试买家二号', '13800000006', '广东深圳市南山区测试地址', NULL, NULL, '2026-08-23 13:20:00', NULL, NULL, '2026-08-23 13:15:00'),
(10, 'JD202608230010', 1, 1, 4999.00, 'COMPLETED', '演示用户', '13800000001', '北京市朝阳区京东课程项目演示地址', '京东快递', 'JDV0010001', '2026-08-22 08:10:00', '2026-08-22 12:30:00', '2026-08-23 17:20:00', '2026-08-22 08:05:00'),
(11, 'JD202608230011', 6, 2, 699.00, 'COMPLETED', '测试买家二号', '13800000006', '广东深圳市南山区测试地址', '顺丰速运', 'SF0011001', '2026-08-19 09:10:00', '2026-08-19 13:30:00', '2026-08-20 18:20:00', '2026-08-19 09:05:00'),
(12, 'JD202608230012', 6, 1, 5999.00, 'COMPLETED', '测试买家二号', '13800000006', '广东深圳市南山区测试地址', '京东快递', 'JDV0012001', '2026-08-17 10:10:00', '2026-08-17 15:30:00', '2026-08-18 19:20:00', '2026-08-17 10:05:00');

INSERT INTO order_item
(id, order_id, product_id, product_name, product_image, price, quantity)
VALUES
(1, 1, 1, 'JDPhone Pro 15', 'https://dummyimage.com/600x450/e2231a/ffffff&text=JDPhone+Pro', 4999.00, 1),
(2, 2, 2, 'JDBook Air 14', 'https://dummyimage.com/600x450/2563eb/ffffff&text=JDBook+Air', 5999.00, 1),
(3, 3, 3, 'JD Smart TV 65', 'https://dummyimage.com/600x450/111827/ffffff&text=JD+Smart+TV', 3299.00, 1),
(4, 4, 8, '主动降噪蓝牙耳机 Max', 'https://dummyimage.com/600x450/0891b2/ffffff&text=ANC+Headset', 699.00, 1),
(5, 5, 9, '机械键盘 K87', 'https://dummyimage.com/600x450/475569/ffffff&text=Keyboard+K87', 259.00, 1),
(6, 6, 5, '京造恒温电热水壶', 'https://dummyimage.com/600x450/059669/ffffff&text=Smart+Kettle', 129.00, 1),
(7, 7, 6, '陕西红富士苹果礼盒', 'https://dummyimage.com/600x450/d97706/ffffff&text=Fresh+Apple', 59.90, 1),
(8, 8, 7, 'Type-C 快充数据线 1.5m', 'https://dummyimage.com/600x450/7c3aed/ffffff&text=USB-C+Cable', 29.90, 1),
(9, 9, 10, '人体工学升降支架', 'https://dummyimage.com/600x450/0f766e/ffffff&text=Laptop+Stand', 169.00, 1),
(10, 10, 1, 'JDPhone Pro 15', '/products/phone.png', 4999.00, 1),
(11, 11, 8, '主动降噪蓝牙耳机 Max', '/products/headphones.png', 699.00, 1),
(12, 12, 2, 'JDBook Air 14', '/products/laptop.png', 5999.00, 1);

INSERT INTO cart_item (id, user_id, product_id, quantity, created_at, updated_at) VALUES
(1, 1, 7, 2, '2026-08-23 08:50:00', '2026-08-23 08:50:00'),
(2, 1, 8, 1, '2026-08-23 08:55:00', '2026-08-23 08:55:00'),
(3, 6, 1, 1, '2026-08-23 12:00:00', '2026-08-23 12:00:00');

INSERT INTO refund_request
(id, order_id, user_id, merchant_id, type, reason, evidence_images, amount, status, merchant_reply, return_logistics_no, admin_decision, admin_remark, created_at, updated_at)
VALUES
(1, 5, 1, 2, 'RETURN_AND_REFUND', '键盘轴体有异常响声，申请退货退款', NULL, 259.00, 'MERCHANT_REVIEWING', NULL, NULL, NULL, NULL, '2026-08-21 09:00:00', '2026-08-21 09:00:00'),
(2, 6, 1, 1, 'RETURN_AND_REFUND', '水壶外观有划痕，申请退货退款', NULL, 129.00, 'WAIT_USER_RETURN', '商家同意退货，请寄回商品', NULL, NULL, NULL, '2026-08-20 10:00:00', '2026-08-20 12:00:00'),
(3, 3, 1, 1, 'REFUND_ONLY', '电视配送时间过长，希望退款', NULL, 3299.00, 'PLATFORM_INTERVENING', '商品已经发货，暂不支持仅退款', NULL, NULL, NULL, '2026-08-22 18:00:00', '2026-08-23 09:00:00'),
(4, 7, 1, 1, 'REFUND_ONLY', '生鲜商品包装破损，平台判定退款', NULL, 59.90, 'REFUND_SUCCESS', '商家同意平台处理', NULL, '同意退款', '凭证充分，支持用户退款', '2026-08-18 18:00:00', '2026-08-19 09:30:00'),
(5, 4, 1, 2, 'RETURN_AND_REFUND', '耳机降噪效果不符合预期', NULL, 699.00, 'MERCHANT_REJECTED', '商品已拆封且无质量问题，拒绝退货', NULL, NULL, NULL, '2026-08-22 19:10:00', '2026-08-22 20:00:00');

INSERT INTO refund_log
(id, refund_id, operator_role, operator_id, action, remark, created_at)
VALUES
(1, 1, 'USER', 1, 'CREATE', '用户提交售后申请：键盘轴体有异常响声', '2026-08-21 09:00:00'),
(2, 2, 'USER', 1, 'CREATE', '用户提交售后申请：水壶外观有划痕', '2026-08-20 10:00:00'),
(3, 2, 'MERCHANT', 2, 'MERCHANT_APPROVE', '商家同意退货，请寄回商品', '2026-08-20 12:00:00'),
(4, 3, 'USER', 1, 'CREATE', '用户提交售后申请：电视配送时间过长', '2026-08-22 18:00:00'),
(5, 3, 'MERCHANT', 2, 'MERCHANT_REJECT', '商品已经发货，暂不支持仅退款', '2026-08-22 20:00:00'),
(6, 3, 'USER', 1, 'USER_REQUEST_INTERVENTION', '用户申请平台客服介入', '2026-08-23 09:00:00'),
(7, 4, 'USER', 1, 'CREATE', '用户提交售后申请：生鲜商品包装破损', '2026-08-18 18:00:00'),
(8, 4, 'SERVICE_ADMIN', 3, 'ADMIN_ARBITRATE', '同意退款：凭证充分，支持用户退款', '2026-08-19 09:30:00'),
(9, 5, 'USER', 1, 'CREATE', '用户提交售后申请：耳机降噪效果不符合预期', '2026-08-22 19:10:00'),
(10, 5, 'MERCHANT', 7, 'MERCHANT_REJECT', '商品已拆封且无质量问题，拒绝退货', '2026-08-22 20:00:00');

INSERT INTO review
(id, order_id, product_id, user_id, rating, content, reply, created_at)
VALUES
(1, 4, 8, 1, 4, '耳机佩戴舒适，降噪效果还可以。', '感谢反馈，我们会持续优化商品描述。', '2026-08-22 19:00:00'),
(2, 7, 6, 1, 5, '水果很新鲜，包装也比较完整。', NULL, '2026-08-19 10:00:00'),
(3, 11, 8, 6, 5, '音质清晰，通勤时降噪很实用，续航也不错。', NULL, '2026-08-21 08:30:00'),
(4, 12, 2, 6, 4, '机身轻薄，日常办公运行很流畅。', '感谢您的认可，祝您使用愉快。', '2026-08-19 11:20:00');

INSERT INTO operation_log
(id, operator_id, operator_role, module, action, detail, created_at)
VALUES
(1, 4, 'PRODUCT_ADMIN', 'PRODUCT', 'APPROVE', '审核通过商品：JDPhone Pro 15', '2026-08-18 09:00:00'),
(2, 4, 'PRODUCT_ADMIN', 'PRODUCT', 'APPROVE', '审核通过商品：JDBook Air 14', '2026-08-18 09:05:00'),
(3, 3, 'SERVICE_ADMIN', 'REFUND', 'ARBITRATE', '管理员仲裁售后单：4', '2026-08-19 09:30:00'),
(4, 2, 'MERCHANT', 'ORDER', 'SHIP', '商家发货：JD202608230003', '2026-08-22 14:20:00');
