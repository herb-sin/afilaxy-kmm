#!/bin/bash
# Script to update npm dependencies and fix vulnerabilities

cd "$(dirname "$0")/functions"

echo "Updating npm dependencies..."
npm update

echo "Running npm audit fix..."
npm audit fix

echo "Running npm audit fix --force (if needed)..."
npm audit fix --force

echo "Done! Check for any remaining vulnerabilities with: npm audit"
