import React, { useState, useMemo } from 'react';

const mockProducts = [
  { id: 1, name: 'أرز بسمتي 5 كجم', price: 15.0, category: 'أغذية أساسية' },
  { id: 2, name: 'حليب المراعي 1 لتر', price: 2.5, category: 'ألبان' },
  { id: 3, name: 'زيت زيتون 500 مل', price: 8.0, category: 'أغذية أساسية' },
  { id: 4, name: 'صابون غسيل 2 كجم', price: 5.0, category: 'منظفات' }
];

export default function PosScreen() {
  const [searchQuery, setSearchQuery] = useState('');
  const [cart, setCart] = useState([]);

  // Filter products based on search query
  const filteredProducts = useMemo(() => {
    return mockProducts.filter((product) =>
      product.name.includes(searchQuery)
    );
  }, [searchQuery]);

  // Calculate total price
  const totalPrice = useMemo(() => {
    return cart.reduce((total, item) => total + item.product.price * item.quantity, 0);
  }, [cart]);

  const addToCart = (product) => {
    setCart((prevCart) => {
      const existingItem = prevCart.find((item) => item.product.id === product.id);
      if (existingItem) {
        return prevCart.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        );
      }
      return [...prevCart, { product, quantity: 1 }];
    });
  };

  const removeFromCart = (productId) => {
    setCart((prevCart) => prevCart.filter((item) => item.product.id !== productId));
  };

  const checkout = () => {
    if (cart.length === 0) {
      alert('السلة فارغة!');
      return;
    }
    alert(`تم الدفع بنجاح! الإجمالي: ${totalPrice} ر.ي`);
    setCart([]);
  };

  return (
    <div style={{ display: 'flex', gap: '20px', padding: '20px', direction: 'rtl', fontFamily: 'Cairo, sans-serif' }}>
      {/* Products Section */}
      <div style={{ flex: 2 }}>
        <h2>المنتجات</h2>
        <input
          type="text"
          placeholder="ابحث عن منتج..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ width: '100%', padding: '10px', marginBottom: '20px', borderRadius: '5px', border: '1px solid #ccc' }}
        />
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: '15px' }}>
          {filteredProducts.map((product) => (
            <div
              key={product.id}
              style={{ padding: '15px', border: '1px solid #ddd', borderRadius: '8px', cursor: 'pointer', textAlign: 'center' }}
              onClick={() => addToCart(product)}
            >
              <h4>{product.name}</h4>
              <p style={{ color: 'green', fontWeight: 'bold' }}>{product.price} ر.ي</p>
            </div>
          ))}
        </div>
      </div>

      {/* Cart Section */}
      <div style={{ flex: 1, borderRight: '2px solid #eee', paddingRight: '20px' }}>
        <h2>سلة المشتريات</h2>
        {cart.length === 0 ? (
          <p>السلة فارغة حالياً</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {cart.map((item) => (
              <div key={item.product.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#f9f9f9', padding: '10px', borderRadius: '5px' }}>
                <div>
                  <strong>{item.product.name}</strong>
                  <div style={{ fontSize: '0.9em', color: '#666' }}>
                    الكمية: {item.quantity} × {item.product.price} ر.ي
                  </div>
                </div>
                <button
                  onClick={() => removeFromCart(item.product.id)}
                  style={{ background: 'red', color: 'white', border: 'none', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer' }}
                >
                  حذف
                </button>
              </div>
            ))}
          </div>
        )}

        <div style={{ marginTop: '20px', paddingTop: '20px', borderTop: '2px dashed #ddd' }}>
          <h3>الإجمالي: <span style={{ color: 'green' }}>{totalPrice} ر.ي</span></h3>
          <button
            onClick={checkout}
            style={{ width: '100%', padding: '15px', background: '#4CAF50', color: 'white', fontSize: '1.1em', border: 'none', borderRadius: '8px', cursor: 'pointer', marginTop: '10px' }}
          >
            إتمام الدفع
          </button>
        </div>
      </div>
    </div>
  );
}
