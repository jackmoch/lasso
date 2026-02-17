#!/usr/bin/env bash
# Bundle Size Analysis Script
# Analyzes production build artifacts and reports sizes

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== Lasso Bundle Size Analysis ===${NC}\n"

# Check if artifacts exist
if [ ! -f "resources/public/js/main.js" ]; then
  echo -e "${YELLOW}Warning: Frontend bundle not found. Run 'bb frontend:build' first.${NC}"
fi

if [ ! -f "resources/public/css/tailwind.css" ]; then
  echo -e "${YELLOW}Warning: CSS bundle not found. Run 'bb css:build' first.${NC}"
fi

if [ ! -f "target/lasso.jar" ]; then
  echo -e "${YELLOW}Warning: Backend JAR not found. Run 'bb uberjar' first.${NC}"
fi

# Frontend bundle analysis
if [ -f "resources/public/js/main.js" ]; then
  JS_SIZE=$(stat -f%z "resources/public/js/main.js" 2>/dev/null || stat -c%s "resources/public/js/main.js")
  JS_SIZE_KB=$((JS_SIZE / 1024))
  JS_GZIP_SIZE=$(gzip -c "resources/public/js/main.js" | wc -c | tr -d ' ')
  JS_GZIP_KB=$((JS_GZIP_SIZE / 1024))

  echo -e "${GREEN}Frontend JavaScript:${NC}"
  echo "  Uncompressed: ${JS_SIZE_KB} KB"
  echo "  Gzipped:      ${JS_GZIP_KB} KB (${BLUE}this is what users download${NC})"

  if [ $JS_SIZE_KB -lt 500 ]; then
    echo -e "  Status: ${GREEN}✓ Excellent (<500KB)${NC}"
  elif [ $JS_SIZE_KB -lt 1000 ]; then
    echo -e "  Status: ${YELLOW}⚠ Good (<1MB)${NC}"
  else
    echo -e "  Status: ${YELLOW}⚠ Large (>1MB) - consider code splitting${NC}"
  fi
  echo ""
fi

# CSS bundle analysis
if [ -f "resources/public/css/tailwind.css" ]; then
  CSS_SIZE=$(stat -f%z "resources/public/css/tailwind.css" 2>/dev/null || stat -c%s "resources/public/css/tailwind.css")
  CSS_SIZE_KB=$((CSS_SIZE / 1024))
  CSS_GZIP_SIZE=$(gzip -c "resources/public/css/tailwind.css" | wc -c | tr -d ' ')
  CSS_GZIP_KB=$((CSS_GZIP_SIZE / 1024))

  echo -e "${GREEN}Frontend CSS:${NC}"
  echo "  Uncompressed: ${CSS_SIZE_KB} KB"
  echo "  Gzipped:      ${CSS_GZIP_KB} KB"

  if [ $CSS_SIZE_KB -lt 20 ]; then
    echo -e "  Status: ${GREEN}✓ Excellent (<20KB)${NC}"
  elif [ $CSS_SIZE_KB -lt 50 ]; then
    echo -e "  Status: ${GREEN}✓ Good (<50KB)${NC}"
  else
    echo -e "  Status: ${YELLOW}⚠ Large - check Tailwind purge configuration${NC}"
  fi
  echo ""
fi

# Backend JAR analysis
if [ -f "target/lasso.jar" ]; then
  JAR_SIZE=$(stat -f%z "target/lasso.jar" 2>/dev/null || stat -c%s "target/lasso.jar")
  JAR_SIZE_MB=$((JAR_SIZE / 1024 / 1024))

  echo -e "${GREEN}Backend JAR:${NC}"
  echo "  Size: ${JAR_SIZE_MB} MB"

  if [ $JAR_SIZE_MB -lt 50 ]; then
    echo -e "  Status: ${GREEN}✓ Good (<50MB)${NC}"
  elif [ $JAR_SIZE_MB -lt 100 ]; then
    echo -e "  Status: ${YELLOW}⚠ Acceptable (<100MB)${NC}"
  else
    echo -e "  Status: ${YELLOW}⚠ Large - consider excluding unused dependencies${NC}"
  fi
  echo ""
fi

# Docker image size (if built)
if command -v docker &> /dev/null; then
  IMAGE_ID=$(docker images -q lasso:latest 2>/dev/null)
  if [ -n "$IMAGE_ID" ]; then
    IMAGE_SIZE=$(docker images lasso:latest --format "{{.Size}}")
    echo -e "${GREEN}Docker Image:${NC}"
    echo "  Size: ${IMAGE_SIZE}"
    echo ""
  fi
fi

# Total initial page load
if [ -f "resources/public/js/main.js" ] && [ -f "resources/public/css/tailwind.css" ]; then
  TOTAL_GZIP=$((JS_GZIP_KB + CSS_GZIP_KB))
  echo -e "${BLUE}Total Initial Page Load (gzipped): ${TOTAL_GZIP} KB${NC}"

  if [ $TOTAL_GZIP -lt 200 ]; then
    echo -e "${GREEN}✓ Excellent - fast initial load${NC}"
  elif [ $TOTAL_GZIP -lt 500 ]; then
    echo -e "${GREEN}✓ Good - acceptable load time${NC}"
  else
    echo -e "${YELLOW}⚠ Consider optimizations for faster initial load${NC}"
  fi
  echo ""
fi

# Recommendations
echo -e "${BLUE}=== Optimization Recommendations ===${NC}"
echo ""
echo "Frontend:"
echo "  • JavaScript is optimized with :advanced compilation"
echo "  • CSS uses Tailwind purge to remove unused styles"
echo "  • Enable gzip/brotli compression on Cloud Run (automatic)"
echo "  • Consider code splitting if bundle grows >1MB"
echo ""
echo "Backend:"
echo "  • JAR includes all dependencies for standalone deployment"
echo "  • Docker layer caching optimizes rebuild times"
echo "  • Cloud Run uses container caching"
echo ""
echo "Performance:"
echo "  • HTTP/2 server push for CSS/JS (if supported)"
echo "  • CDN for static assets (optional for global audience)"
echo "  • Browser caching headers (set in server)"
echo ""
