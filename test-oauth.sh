#!/bin/bash
# Quick OAuth test script

echo "Testing Last.fm OAuth flow..."
echo ""
echo "1. Getting token from Last.fm API..."

# Get token
RESPONSE=$(curl -s "https://ws.audioscrobbler.com/2.0/?method=auth.getToken&api_key=e38eb1dc11a1100ffdeb0c48620e3885&format=json")
echo "Response: $RESPONSE"
echo ""

# Extract token (using basic grep/sed)
TOKEN=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
echo "Token: $TOKEN"
echo ""

# Generate auth URL
AUTH_URL="https://www.last.fm/api/auth/?api_key=e38eb1dc11a1100ffdeb0c48620e3885&token=$TOKEN&cb=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fauth%2Fcallback"
echo "Auth URL (copy and paste this into your browser):"
echo ""
echo "$AUTH_URL"
echo ""
echo "After authorizing, Last.fm should redirect to:"
echo "http://localhost:8080/api/auth/callback?token=$TOKEN"
