// Firebase Authentication Configuration

// Initialize Firebase (already done in login.html, but this is for reference)
let auth;
if (typeof firebase !== 'undefined') {
    auth = firebase.auth();
}

// Facebook Provider
const facebookProvider = new firebase.auth.FacebookAuthProvider();
facebookProvider.addScope('public_profile');
facebookProvider.setCustomParameters({
    'display': 'popup'
});

// Google Provider
const googleProvider = new firebase.auth.GoogleAuthProvider();
googleProvider.addScope('profile');
googleProvider.addScope('email');

// Sign in with Facebook Popup
async function signInWithFacebook() {
    try {
        const result = await auth.signInWithPopup(facebookProvider);
        const credential = firebase.auth.FacebookAuthProvider.credential(
            result.credential.accessToken
        );
        
        // Get Firebase ID token
        const idToken = await result.user.getIdToken();
        
        // Send token to backend
        await sendTokenToBackend(idToken, 'facebook');
        
    } catch (error) {
        console.error("Facebook sign-in error:", error);
        
        if (error.code === 'auth/account-exists-with-different-credential') {
            alert("Tài khoản đã tồn tại với phương thức đăng nhập khác.");
        } else if (error.code === 'auth/popup-closed-by-user') {
            console.log("User closed the popup");
        } else {
            alert("Đăng nhập Facebook thất bại: " + error.message);
        }
    }
}

// Sign in with Google Popup
async function signInWithGoogle() {
    try {
        const result = await auth.signInWithPopup(googleProvider);
        
        // Get Firebase ID token
        const idToken = await result.user.getIdToken();
        
        // Send token to backend
        await sendTokenToBackend(idToken, 'google');
        
    } catch (error) {
        console.error("Google sign-in error:", error);
        
        if (error.code === 'auth/popup-closed-by-user') {
            console.log("User closed the popup");
        } else {
            alert("Đăng nhập Google thất bại: " + error.message);
        }
    }
}

// Send Firebase token to backend for verification
async function sendTokenToBackend(idToken, provider) {
    try {
        const response = await fetch('/auth/firebase-login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                idToken: idToken,
                provider: provider
            })
        });
        
        if (response.ok) {
            // Login successful, redirect to home
            window.location.href = '/';
        } else {
            const errorData = await response.json();
            alert("Đăng nhập thất bại: " + (errorData.error || 'Unknown error'));
        }
    } catch (error) {
        console.error("Error sending token to backend:", error);
        alert("Đã xảy ra lỗi khi đăng nhập. Vui lòng thử lại.");
    }
}

// Listen for auth state changes (optional, for debugging)
auth.onAuthStateChanged((user) => {
    if (user) {
        console.log("Firebase user is signed in:", user.uid, user.email);
    } else {
        console.log("Firebase user is signed out");
    }
});
