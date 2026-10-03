"""Minimal settings for the local article analysis API."""

import os

DEBUG = True
SECRET_KEY = os.getenv("DJANGO_SECRET_KEY", "unsafe-local-development-only")
ALLOWED_HOSTS = ["localhost", "127.0.0.1", "10.0.2.2", "testserver"]

ROOT_URLCONF = "server.urls"
INSTALLED_APPS = []
MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]
DATABASES = {}
TEMPLATES = []
WSGI_APPLICATION = "server.wsgi.application"
DATA_UPLOAD_MAX_MEMORY_SIZE = 8192

SECURE_CONTENT_TYPE_NOSNIFF = True
