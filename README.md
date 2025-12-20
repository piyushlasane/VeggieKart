# 🥬 VeggieKart - Fresh Grocery Delivery App

<div align="center">
  
  ![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
  ![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)
  ![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
  ![Firebase](https://img.shields.io/badge/Backend-Firebase-orange?logo=firebase)
  
</div>

---

## 📱 Screenshots

### Authentication Flow
<p float="left">
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232916/Auth_-_VeggieKart_jnbkdn.jpg" width="250" alt="Auth Screen"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232916/Login_-_VeggieKart_an7tyt.jpg" width="250" alt="Login Screen"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232913/Profile_-_VeggieKart_wuiua9.jpg" width="250" alt="Complete Profile"/>
</p>

### Main Application
<p float="left">
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232916/Homepage_-_VeggieKart_wmonjw.jpg" width="250" alt="Home Screen"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232917/Category_-_VeggieKart_meuial.jpg" width="250" alt="Categories"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232914/Product_-_VeggieKart_srqlkn.jpg" width="250" alt="Product Details"/>
</p>

### Address Management
<p float="left">
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232914/Address_-_VeggieKart_qzbhco.jpg" width="250" alt="Manage Addresses"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232914/AddAdd_-_VeggieKart_atuyni.jpg" width="250" alt="Add Address"/>
  &nbsp;&nbsp;&nbsp;
  <img src="https://res.cloudinary.com/dosrhyslq/image/upload/v1766232914/Selection_-_VeggieKart_lprryp.jpg" width="250" alt="Address Selection"/>
</p>

---

## 📖 About

**VeggieKart** is a modern Android application built with cutting-edge Android technologies, the app provides users with a seamless shopping experience featuring real-time product catalogs, secure authentication, and intelligent address management.

The app demonstrates production-ready architecture with Firebase integration, location services, and a beautiful Material Design 3 UI built entirely with Jetpack Compose.

---

## ✨ Features

### 🔐 Authentication & User Management
- **OTP-based Authentication**: Secure Firebase phone authentication with automatic OTP verification
- **Profile Management**: Create and update user profiles with real-time sync
- **Guest Mode**: Browse products without signing in

### 🏠 Smart Address Management
- **Multiple Addresses**: Add, edit, and manage multiple delivery addresses
- **Auto-Location Detection**: Automatically detect and fill address using GPS
- **Default Address**: Set preferred delivery address with one tap
- **Address Types**: Categorize addresses as Home, Work, or Other

### 🛒 Product Catalog
- **Category Browsing**: Explore products organized by categories
- **Product Details**: View detailed product information with image galleries
- **Real-time Updates**: Product data synced in real-time from Firebase
- **Dynamic Banners**: Promotional banners with auto-scrolling

### 🎨 Modern UI/UX
- **Material Design 3**: Latest Material You design principles
- **Jetpack Compose**: Fully declarative UI with smooth animations
- **Bottom Navigation**: Intuitive navigation between Home, Categories, and Cart
- **Responsive Design**: Optimized for different screen sizes

### 🌐 Backend Integration
- **Firebase Firestore**: NoSQL database for real-time data sync
- **Firebase Auth**: Secure phone-based authentication
- **Cloud Storage**: Efficient image loading with Coil
- **Security Rules**: Data protection with Firestore security rules

---

## 🚀 Technologies Used

### 💻 Android Development
- **Kotlin** — Modern, concise, and safe programming language
- **Jetpack Compose** — Declarative UI toolkit for native Android
- **MVVM Architecture** — Clean separation of concerns
- **Coroutines** — Asynchronous programming for smooth UI
- **Navigation Component** — Type-safe navigation between screens
- **Android Studio** — Official IDE for Android development

### 🔥 Firebase Services
- **Firebase Authentication** — Phone OTP-based secure login
- **Cloud Firestore** — Real-time NoSQL database
- **Firebase Storage** — Cloud storage for product images

### 📚 Libraries & Dependencies
- **Coil** — Modern image loading library for Android
- **Material 3** — Latest Material Design components
- **Google Play Services** — Location services integration
- **ViewPager Dots Indicator** — Elegant page indicators

---

## 🎨 User Interface Overview

| Screen | Description |
|--------|-------------|
| 🔐 **Auth Screen** | Welcome screen with login/guest options |
| 📱 **Login Screen** | Phone number input with OTP verification |
| ✏️ **Complete Profile** | First-time user profile setup |
| 🏠 **Home Screen** | Product banners, categories, and featured items |
| 📦 **Categories** | Browse products by category |
| 🛍️ **Product Details** | Detailed product view with image gallery |
| 👤 **Profile Screen** | User profile with settings and logout |
| ✏️ **Edit Profile** | Update user information |
| 📍 **Address Management** | Add, edit, delete, and set default addresses |
| 🗺️ **Add Address** | Location-based address input with auto-detection |

---

## 🏗️ App Architecture

```
VeggieKart/
├── components/           # Reusable UI components
│   ├── AddressSelectionBottomSheet.kt
│   ├── BannerView.kt
│   ├── CategoriesView.kt
│   ├── HeaderView.kt
│   └── ProductItemView.kt
├── pages/               # Feature pages
│   ├── HomePage.kt
│   ├── CategoryProductsPage.kt
│   └── ProductDetailsPage.kt
├── screens/             # Main screens
│   ├── AuthScreen.kt
│   ├── LoginScreen.kt
│   ├── HomeScreen.kt
│   ├── ProfileScreen.kt
│   ├── AddAddressScreen.kt
│   └── ManageAddressesScreen.kt
├── model/              # Data models
│   ├── UserModel.kt
│   ├── ProductModel.kt
│   ├── CategoryModel.kt
│   └── AddressModel.kt
├── viewmodel/          # ViewModels (MVVM)
│   └── AuthViewModel.kt
└── ui/theme/           # Material Design theming
```

**Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Model**: Data classes and Firebase operations
- **View**: Composable functions (UI layer)
- **ViewModel**: Business logic and state management

---

## 🗄️ Database Structure

### Firestore Collections

```
firestore/
├── users/
│   └── {uid}/
│       ├── name: String
│       ├── phone: String
│       ├── createdAt: Timestamp
│       ├── addresses: Array<AddressModel>
│       └── cartItems: Map<productId, quantity>
│
└── data/
    ├── banners/
    │   └── urls: Array<String>
    └── stock/
        ├── categories/
        │   └── {categoryId}/
        │       ├── id: String
        │       ├── name: String
        │       └── imageUrl: String
        └── products/
            └── {productId}/
                ├── id: String
                ├── title: String
                ├── description: String
                ├── category: String
                ├── price: String
                ├── actualPrice: String
                └── images: Array<String>
```

---

## 🚧 Roadmap & Planned Features

### 🛒 Cart & Checkout (In Progress)
- Data structure already implemented in `UserModel`
- Add to cart functionality
- Cart quantity management
- Checkout flow with order placement

### 📦 Order Management
- Order history
- Order tracking
- Re-order functionality

### 🔍 Enhanced Search
- Product search with filters
- Category-based filtering
- Price range filters

### ❤️ Wishlist
- Save favorite products
- Quick access to saved items

---

## 🛠️ Setup & Installation

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17 or higher
- Android SDK 34 (Android 14)
- Firebase project with Authentication and Firestore enabled

### Installation Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/veggiekart.git
   cd veggiekart
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository

3. **Configure Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Add an Android app to your Firebase project
   - Download `google-services.json`
   - Place it in `app/` directory
   - Enable Phone Authentication in Firebase Console
   - Enable Firestore Database

4. **Sync Gradle**
   - Wait for Gradle sync to complete
   - Resolve any dependency issues

5. **Run the App**
   - Connect an Android device or start an emulator
   - Click "Run" or press `Shift + F10`

---

## 📦 Dependencies

```kotlin
dependencies {
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Firebase
    implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.9.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")
}
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

© 2025 Piyush Lasane. All rights reserved.

This project is for educational and portfolio purposes only.

---

## 👨‍💻 Developer

<table>
  <tbody>
    <tr>
      <td align="center">
        <a href="https://github.com/piyushlasane">
          <img src="https://github.com/piyushlasane.png" width="100" alt="Piyush Lasane"/><br />
          <sub><b>Piyush Lasane</b></sub>
        </a><br />
        <a href="#" title="Code">💻</a>
        <a href="#" title="Design">🎨</a>
        <a href="#" title="Architecture">🏗️</a>
        <a href="#" title="Documentation">📖</a>
      </td>
    </tr>
  </tbody>
</table>

---

## 📞 Contact

- **Email**: piyushlasane@gmail.com
- **LinkedIn**: https://www.linkedin.com/in/piyushlasane/
- **GitHub**: https://github.com/piyushlasane/

---

<div align="center">
  <p>⭐ Star this repository if you found it helpful!</p>
</div>
