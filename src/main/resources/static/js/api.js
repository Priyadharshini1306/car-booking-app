// =============================================
// api.js — Centralized API helper
//
// FIX (Bug 4+5): Booking page shows error
//
// ROOT CAUSE: Two possible reasons:
//
// 1. The token in localStorage is expired or
//    invalid (because of the old short JWT_SECRET).
//    After fixing JWT_SECRET to 64 chars, new
//    tokens will work correctly.
//
// 2. The Authorization header was being sent
//    but the token was invalid, so the server
//    returned 401/403. The frontend caught this
//    as a generic error and showed "Booking
//    cannot be loaded".
//
// FIX: Added a 401/403 interceptor in apiFetch().
//    If any API call returns 401 or 403, it means
//    the token is expired — clear localStorage
//    and redirect to login. This prevents the
//    "stuck" state where the user appears logged
//    in but all API calls fail silently.
// =============================================

const API_BASE = '/api';

function getToken() {
  return localStorage.getItem('token');
}

// CENTRAL FETCH WITH AUTH + 401 HANDLING
async function apiFetch(url, options = {}) {
  var token = getToken();
  var headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': 'Bearer ' + token } : {}),
    ...options.headers
  };

  var res = await fetch(API_BASE + url, { ...options, headers });

  // FIX: Token expired or invalid → clear and redirect to login
  if (res.status === 401 || res.status === 403) {
    localStorage.clear();
    window.location.href = '/login.html';
    throw new Error('Session expired. Please login again.');
  }

  if (!res.ok) {
    var err = await res.text();
    throw new Error(err || 'Request failed');
  }

  return res.json();
}

// AUTH
const login = (email, password) =>
  apiFetch('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });

const register = (data) =>
  apiFetch('/auth/register', { method: 'POST', body: JSON.stringify(data) });

// CARS
const getCars = () => apiFetch('/cars/public/available');

const filterCars = (type, seats) => {
  var url = '/cars/public/filter?';
  if (type)  url += 'type='  + type  + '&';
  if (seats) url += 'seats=' + seats + '&';
  return apiFetch(url);
};

const getCarById = (id) => apiFetch('/cars/public/' + id);

// BOOKINGS
const createBooking = (data) =>
  apiFetch('/bookings', { method: 'POST', body: JSON.stringify(data) });

const getMyBookings = () => apiFetch('/bookings/my');

const cancelBooking = (id) =>
  apiFetch('/bookings/' + id + '/cancel', { method: 'PUT' });

// FEEDBACK
const submitFeedback = (data) =>
  apiFetch('/feedback', { method: 'POST', body: JSON.stringify(data) });

const getAllFeedback = () =>
  fetch('/api/feedback/public/all').then(function(res) {
    if (!res.ok) throw new Error('Failed to load feedback');
    return res.json();
  });

// CHAT
const sendMessage = (message) =>
  apiFetch('/chat/send', { method: 'POST', body: JSON.stringify({ message }) });

const getChatHistory = () => apiFetch('/chat/history');

// ADMIN
const adminStats = () => apiFetch('/admin/stats');
const adminGetCars = () => apiFetch('/admin/cars');
const adminAddCar = (car) => apiFetch('/admin/cars', { method: 'POST', body: JSON.stringify(car) });
const adminUpdateCar = (id, car) => apiFetch('/admin/cars/' + id, { method: 'PUT', body: JSON.stringify(car) });
const adminDeleteCar = (id) => apiFetch('/admin/cars/' + id, { method: 'DELETE' });
const adminGetDrivers = () => apiFetch('/admin/drivers');
const adminAddDriver = (driver) => apiFetch('/admin/drivers', { method: 'POST', body: JSON.stringify(driver) });
const adminUpdateDriver = (id, driver) => apiFetch('/admin/drivers/' + id, { method: 'PUT', body: JSON.stringify(driver) });
const adminDeleteDriver = (id) => apiFetch('/admin/drivers/' + id, { method: 'DELETE' });
const adminGetBookings = () => apiFetch('/admin/bookings');
const adminUpdateBookingStatus = (id, body) =>
  apiFetch('/admin/bookings/' + id + '/status', { method: 'PUT', body: JSON.stringify(body) });