USE jd_clone_mall;

INSERT INTO user (id, username, password, nickname, phone, role, status) VALUES
(1, 'user001', '123456', '演示用户', '13800000001', 'USER', 'NORMAL'),
(2, 'merchant001', '123456', '京东自营商家', '13800000002', 'MERCHANT', 'NORMAL'),
(3, 'service_admin', '123456', '客服管理员', '13800000003', 'SERVICE_ADMIN', 'NORMAL'),
(4, 'product_admin', '123456', '商品审核员', '13800000004', 'PRODUCT_ADMIN', 'NORMAL'),
(5, 'super_admin', '123456', '超级管理员', '13800000005', 'SUPER_ADMIN', 'NORMAL');

INSERT INTO merchant (id, user_id, company_name, contact_name, contact_phone, status) VALUES
(1, 2, '京东课程项目示例商家', '张三', '13800000002', 'APPROVED');

INSERT INTO shop (id, merchant_id, name, description, status) VALUES
(1, 1, '京东自营演示店', '用于课程答辩演示的商家店铺', 'NORMAL');

INSERT INTO category (id, name, parent_id, sort_order) VALUES
(1, '手机数码', 0, 1),
(2, '电脑办公', 0, 2),
(3, '家用电器', 0, 3);

INSERT INTO product
(id, merchant_id, category_id, name, subtitle, main_image, price, stock, sales, audit_status, shelf_status)
VALUES
(1, 1, 1, 'JDPhone Pro 15', '高刷屏幕，长续航，适合演示下单流程', 'https://dummyimage.com/600x450/e2231a/ffffff&text=JDPhone+Pro', 4999.00, 80, 0, 'APPROVED', 'ON'),
(2, 1, 2, 'JDBook Air 14', '轻薄办公本，适合学生课程项目展示', 'https://dummyimage.com/600x450/2563eb/ffffff&text=JDBook+Air', 5999.00, 45, 0, 'APPROVED', 'ON'),
(3, 1, 3, 'JD Smart TV 65', '大屏电视，模拟家电类商品', 'https://dummyimage.com/600x450/111827/ffffff&text=JD+Smart+TV', 3299.00, 30, 0, 'APPROVED', 'ON'),
(4, 1, 1, '待审核无线耳机', '商家新提交，等待管理员审核', 'https://dummyimage.com/600x450/f59e0b/ffffff&text=Pending+Earbuds', 399.00, 120, 0, 'PENDING', 'OFF');

INSERT INTO address (id, user_id, receiver, phone, province, city, detail, is_default) VALUES
(1, 1, '演示用户', '13800000001', '北京', '北京市', '朝阳区京东课程项目演示地址', 1);
