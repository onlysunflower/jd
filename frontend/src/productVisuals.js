const imagesByProductId = {
  1: '/products/jdphone-pro-15.png',
  2: '/products/jdbook-air-14.png',
  3: '/products/smart-tv.png',
  5: '/products/kettle.png',
  6: '/products/apple-gift-box.png',
  7: '/products/usb-c-cable.png',
  8: '/products/anc-headphones-max.png',
  9: '/products/mechanical-keyboard.png',
  10: '/products/laptop-stand.png'
}

const imagesByCategoryId = {
  1: '/products/phone.png',
  2: '/products/laptop.png',
  3: '/products/kettle.png',
  4: '/products/kettle.png',
  5: '/products/kettle.png'
}

export function productImage(product) {
  return imagesByProductId[product?.id] || imagesByCategoryId[product?.categoryId] || '/products/catalog-collection.png'
}
