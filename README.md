# 🥬 VeggieKart

A full-featured grocery delivery Android app — built with **Kotlin** and **Jetpack Compose** — covering the complete flow from browsing to checkout: OTP login, live product catalog, cart, address management, **real Razorpay payments**, and order history, backed by Firebase and a custom payment backend.

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange?logo=firebase)
![Razorpay](https://img.shields.io/badge/Payments-Razorpay-0b2447)

</div>

---

## 📱 Preview

<p float="left">
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232916/Auth_-_VeggieKart_jnbkdn.jpg" width="220" alt="Auth Screen"/>
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232916/Homepage_-_VeggieKart_wmonjw.jpg" width="220" alt="Home Screen"/>
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232914/Product_-_VeggieKart_srqlkn.jpg" width="220" alt="Product Details"/>
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232914/Address_-_VeggieKart_qzbhco.jpg" width="220" alt="Address Management"/>
</p>

---

## ✨ What It Does

**Auth & Profile**
- Firebase Phone OTP authentication, guest browsing, first-time profile setup

**Catalog**
- Real-time product & category listing from Firestore, auto-scrolling banners, detailed product pages

**Cart**
- Firestore-transaction-backed add/update/remove so concurrent taps can't corrupt quantities
- Gracefully flags items that were removed from stock instead of silently dropping them

**Checkout & Payments**
- End-to-end **Razorpay integration**: order creation and signature verification happen on a dedicated backend, never on-device
- Payment state machine (`Idle → CreatingOrder → AwaitingPayment → VerifyingPayment → Success/Failed`) with automatic retry-with-backoff if verification is slow
- Explicit safeguard against double-charging: if payment succeeds but backend confirmation fails, the UI only allows retrying verification — never re-initiating payment

**Orders**
- Order history with a snapshot of the delivery address and item list captured at time of purchase, so later address edits never rewrite past orders

**Addresses**
- Multiple saved addresses, GPS auto-detect, default address, Home/Work/Other tagging

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM + Repository pattern |
| Backend | Firebase Auth, Cloud Firestore, Firebase Storage |
| Payments | Razorpay SDK + Retrofit against a custom order/verify backend |
| Async | Kotlin Coroutines, StateFlow |
| Image Loading | Coil |
| Location | Google Play Services (Location) |

---

## 🏗️ Architecture

```
com.project.veggiekart/
├── components/     # Reusable UI components
├── pages/          # Feature pages (Home, Cart, Orders, Product Details)
├── screens/        # Top-level screens (Auth, Checkout, Addresses, Profile)
├── model/          # Data models (User, Product, Address, Order)
├── viewmodel/       # MVVM ViewModels (Auth, Cart, Order)
├── repository/      # Firestore data access, isolated from ViewModels
├── network/          # Retrofit client + payment API contracts
├── payment/          # Razorpay launcher & result handling
└── ui/theme/         # Material Design theming
```

Firestore holds `users` (profile, addresses, cart) and `data` (banners, categories, products), plus a top-level `orders` collection written by the backend after payment signature verification.

---

## 👨‍💻 Developer

**Piyush Lasane**
[GitHub](https://github.com/piyushlasane) · [LinkedIn](https://www.linkedin.com/in/piyushlasane/) · piyushlasane@gmail.com

---

<div align="center">
  <sub>© 2026 Piyush Lasane — built as a personal/portfolio project</sub>
</div>