import React, { useEffect, useMemo, useState } from 'react';
import { createOrder, getCategories, getOrders, getProducts } from './api';

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

function formatCurrency(value) {
  return currencyFormatter.format(Number(value));
}

function Stars({ rating }) {
  const fullStars = Math.round(rating);
  return <span className="stars">{'★'.repeat(fullStars)}{'☆'.repeat(5 - fullStars)}</span>;
}

export default function App() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [orders, setOrders] = useState([]);
  const [cart, setCart] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [checkout, setCheckout] = useState({
    customerName: '',
    email: '',
    address: '',
  });
  const [status, setStatus] = useState({ kind: 'idle', message: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const [productData, categoryData, orderData] = await Promise.all([
          getProducts(),
          getCategories(),
          getOrders(),
        ]);
        setProducts(productData);
        setCategories(['All', ...categoryData]);
        setOrders(orderData);
      } catch (error) {
        setStatus({ kind: 'error', message: error.message });
      }
    }

    load();
  }, []);

  useEffect(() => {
    const savedCart = window.localStorage.getItem('marketplace-cart');
    if (savedCart) {
      try {
        setCart(JSON.parse(savedCart));
      } catch {
        window.localStorage.removeItem('marketplace-cart');
      }
    }
  }, []);

  useEffect(() => {
    window.localStorage.setItem('marketplace-cart', JSON.stringify(cart));
  }, [cart]);

  const filteredProducts = useMemo(() => {
    const query = search.trim().toLowerCase();
    return products.filter((product) => {
      const matchesSearch =
        !query ||
        product.name.toLowerCase().includes(query) ||
        product.description.toLowerCase().includes(query);
      const matchesCategory =
        selectedCategory === 'All' || product.category === selectedCategory;
      return matchesSearch && matchesCategory;
    });
  }, [products, search, selectedCategory]);

  const cartItems = useMemo(() => {
    return cart
      .map((item) => {
        const product = products.find((candidate) => candidate.id === item.productId);
        if (!product) {
          return null;
        }
        return {
          ...item,
          product,
          lineTotal: product.price * item.quantity,
        };
      })
      .filter(Boolean);
  }, [cart, products]);

  const subtotal = cartItems.reduce((sum, item) => sum + item.lineTotal, 0);
  const shipping = subtotal > 0 ? 12 : 0;
  const total = subtotal + shipping;

  function addToCart(product) {
    setCart((current) => {
      const existing = current.find((item) => item.productId === product.id);
      if (existing) {
        return current.map((item) =>
          item.productId === product.id
            ? { ...item, quantity: Math.min(item.quantity + 1, product.stock) }
            : item,
        );
      }
      return [...current, { productId: product.id, quantity: 1 }];
    });
    setStatus({ kind: 'success', message: `${product.name} added to cart` });
  }

  function updateQuantity(productId, quantity) {
    setCart((current) =>
      current
        .map((item) => (item.productId === productId ? { ...item, quantity } : item))
        .filter((item) => item.quantity > 0),
    );
  }

  async function handleCheckout(event) {
    event.preventDefault();
    if (!cartItems.length) {
      setStatus({ kind: 'error', message: 'Your cart is empty.' });
      return;
    }

    setIsSubmitting(true);
    setStatus({ kind: 'idle', message: '' });

    try {
      const order = await createOrder({
        ...checkout,
        items: cartItems.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
      });

      const updatedProducts = await getProducts();
      setOrders((current) => [order, ...current]);
      setProducts(updatedProducts);
      setCart([]);
      setCheckout({ customerName: '', email: '', address: '' });
      setStatus({
        kind: 'success',
        message: `Order #${order.id} confirmed for ${formatCurrency(order.total)}.`,
      });
      window.localStorage.removeItem('marketplace-cart');
    } catch (error) {
      setStatus({ kind: 'error', message: error.message });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="shell">
      <header className="hero">
        <div>
          <p className="eyebrow">React + Java</p>
          <h1>MarketPlace</h1>
          <p className="hero-copy">
            A polished e-commerce starter with a React storefront and a Spring Boot API.
          </p>
        </div>
        <div className="hero-card">
          <span>Live catalog</span>
          <strong>{products.length} products</strong>
          <span>{orders.length} orders placed in this session</span>
        </div>
      </header>

      {status.message ? (
        <div className={`notice ${status.kind}`}>{status.message}</div>
      ) : null}

      <main className="layout">
        <section className="panel panel-wide">
          <div className="panel-heading">
            <div>
              <p className="section-label">Catalog</p>
              <h2>Shop the collection</h2>
            </div>
            <input
              className="search"
              type="search"
              placeholder="Search products"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>

          <div className="filters">
            {categories.map((category) => (
              <button
                key={category}
                className={category === selectedCategory ? 'chip active' : 'chip'}
                onClick={() => setSelectedCategory(category)}
              >
                {category}
              </button>
            ))}
          </div>

          <div className="grid">
            {filteredProducts.map((product) => (
              <article className="card" key={product.id}>
                <div className="card-image">
                  <img src={product.imageUrl} alt={product.name} />
                </div>
                <div className="card-body">
                  <div className="meta">
                    <span>{product.category}</span>
                    <span>{product.stock} in stock</span>
                  </div>
                  <h3>{product.name}</h3>
                  <p>{product.description}</p>
                  <div className="rating-row">
                    <Stars rating={product.rating} />
                    <strong>{product.rating.toFixed(1)}</strong>
                  </div>
                  <div className="card-footer">
                    <strong>{formatCurrency(product.price)}</strong>
                    <button onClick={() => addToCart(product)}>Add to cart</button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>

        <aside className="sidebar">
          <section className="panel">
            <div className="panel-heading compact">
              <div>
                <p className="section-label">Cart</p>
                <h2>Your items</h2>
              </div>
              <strong>{cartItems.length}</strong>
            </div>

            <div className="cart-list">
              {cartItems.length ? (
                cartItems.map((item) => (
                  <div className="cart-item" key={item.productId}>
                    <div>
                      <h3>{item.product.name}</h3>
                      <p>{formatCurrency(item.product.price)} each</p>
                    </div>
                    <div className="quantity">
                      <button onClick={() => updateQuantity(item.productId, item.quantity - 1)}>
                        -
                      </button>
                      <span>{item.quantity}</span>
                      <button
                        onClick={() =>
                          updateQuantity(
                            item.productId,
                            Math.min(item.quantity + 1, item.product.stock),
                          )
                        }
                      >
                        +
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <p className="empty">Your cart is empty. Add a few products to start.</p>
              )}
            </div>

            <div className="summary">
              <div><span>Subtotal</span><strong>{formatCurrency(subtotal)}</strong></div>
              <div><span>Shipping</span><strong>{formatCurrency(shipping)}</strong></div>
              <div className="total"><span>Total</span><strong>{formatCurrency(total)}</strong></div>
            </div>
          </section>

          <section className="panel">
            <div className="panel-heading compact">
              <div>
                <p className="section-label">Checkout</p>
                <h2>Place your order</h2>
              </div>
            </div>

            <form className="checkout" onSubmit={handleCheckout}>
              <input
                type="text"
                placeholder="Full name"
                value={checkout.customerName}
                onChange={(event) =>
                  setCheckout((current) => ({ ...current, customerName: event.target.value }))
                }
                required
              />
              <input
                type="email"
                placeholder="Email address"
                value={checkout.email}
                onChange={(event) =>
                  setCheckout((current) => ({ ...current, email: event.target.value }))
                }
                required
              />
              <textarea
                placeholder="Shipping address"
                value={checkout.address}
                onChange={(event) =>
                  setCheckout((current) => ({ ...current, address: event.target.value }))
                }
                required
              />
              <button className="primary" type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Placing order...' : 'Complete purchase'}
              </button>
            </form>
          </section>

          <section className="panel">
            <div className="panel-heading compact">
              <div>
                <p className="section-label">Orders</p>
                <h2>Recent purchases</h2>
              </div>
            </div>

            <div className="orders">
              {orders.length ? (
                orders.slice(0, 3).map((order) => (
                  <article key={order.id} className="order-card">
                    <div className="order-top">
                      <strong>#{order.id}</strong>
                      <span>{order.status}</span>
                    </div>
                    <p>{order.customerName}</p>
                    <small>{new Date(order.createdAt).toLocaleString()}</small>
                    <strong>{formatCurrency(order.total)}</strong>
                  </article>
                ))
              ) : (
                <p className="empty">No orders yet. Your first checkout will appear here.</p>
              )}
            </div>
          </section>
        </aside>
      </main>
    </div>
  );
}
