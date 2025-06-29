# Aadhaar Verification Integration

This document describes the Aadhaar verification integration for the User Management Service.

## Overview

The Aadhaar verification system provides digital integration for India's Aadhaar verification through various methods including OTP-based verification and basic verification. The system is designed to be compliant with UIDAI guidelines and includes proper logging, masking, and audit trails.

## Features

- **OTP-based Verification**: Generate and verify OTP for Aadhaar verification
- **Basic Verification**: Direct verification with Aadhaar details
- **Verification History**: Track all verification attempts
- **Audit Trail**: Complete logging and masking of sensitive data
- **Status Tracking**: Monitor verification status and progress
- **User Integration**: Link verifications to user accounts

## API Endpoints

### 1. Generate Transaction ID
```
GET /api/aadhaar/transaction-id
```
Generates a unique transaction ID for Aadhaar operations.

**Response:**
```json
{
  "success": true,
  "message": "Transaction ID generated",
  "data": "TXN_1703123456789_abc12345",
  "timestamp": "2023-12-21T10:30:45"
}
```

### 2. Generate OTP
```
POST /api/aadhaar/generate-otp
```
Generates an OTP for Aadhaar verification.

**Request Body:**
```json
{
  "aadhaarNumber": "123456789012",
  "transactionId": "TXN_1703123456789_abc12345",
  "consent": "Y",
  "purpose": "Authentication"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP generated successfully",
  "data": {
    "transactionId": "TXN_1703123456789_abc12345",
    "verificationId": "uuid-verification-id",
    "status": "SUCCESS",
    "message": "OTP sent successfully"
  }
}
```

### 3. Verify OTP
```
POST /api/aadhaar/verify-otp
```
Verifies the OTP and retrieves Aadhaar details.

**Request Body:**
```json
{
  "otp": "123456",
  "transactionId": "TXN_1703123456789_abc12345",
  "aadhaarNumber": "123456789012"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Aadhaar verification successful",
  "data": {
    "verified": true,
    "verificationId": "uuid-verification-id",
    "aadhaarNumber": "1234****5678",
    "name": "John Doe",
    "dateOfBirth": "01/01/1990",
    "gender": "MALE",
    "address": "Sample Address",
    "verifiedAt": "2023-12-21T10:35:45",
    "verificationMethod": "OTP",
    "nameMatch": "100",
    "dobMatch": "100"
  }
}
```

### 4. Basic Verification
```
POST /api/aadhaar/verify
```
Performs basic Aadhaar verification (demo mode).

**Request Body:**
```json
{
  "aadhaarNumber": "123456789012",
  "name": "John Doe",
  "dateOfBirth": "01/01/1990",
  "gender": "MALE",
  "address": "Sample Address"
}
```

### 5. Get Verification Status
```
GET /api/aadhaar/status/{verificationId}
```
Retrieves the status of a specific verification.

### 6. Get Verification History
```
GET /api/aadhaar/history/{userId}
```
Retrieves verification history for a specific user.

## Configuration

### Environment Variables

Add the following environment variables to your application:

```properties
# Aadhaar API Configuration
AADHAAR_API_BASE_URL=https://api.uidai.gov.in
AADHAAR_CLIENT_ID=your-client-id
AADHAAR_CLIENT_SECRET=your-client-secret
AADHAAR_APP_ID=your-app-id
AADHAAR_CONSENT_TEXT=Y
```

### Application Properties

The following properties are configured in `application.properties`:

```properties
# Aadhaar API Configuration
aadhaar.api.base-url=${AADHAAR_API_BASE_URL:https://api.uidai.gov.in}
aadhaar.api.client-id=${AADHAAR_CLIENT_ID:your-client-id}
aadhaar.api.client-secret=${AADHAAR_CLIENT_SECRET:your-client-secret}
aadhaar.api.app-id=${AADHAAR_APP_ID:your-app-id}
aadhaar.api.consent-text=${AADHAAR_CONSENT_TEXT:Y}
```

## Database Schema

### User Table Updates

The `users` table has been extended with Aadhaar-related fields:

```sql
ALTER TABLE users ADD COLUMN aadhaar_number VARCHAR(255);
ALTER TABLE users ADD COLUMN aadhaar_verified BOOLEAN;
ALTER TABLE users ADD COLUMN aadhaar_verification_id VARCHAR(255);
ALTER TABLE users ADD COLUMN aadhaar_verified_at TIMESTAMP;
```

### Aadhaar Verification Table

A new table `aadhaar_verifications` tracks all verification attempts:

```sql
CREATE TABLE aadhaar_verifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    verification_id VARCHAR(255) UNIQUE,
    aadhaar_number VARCHAR(255),
    user_id BIGINT,
    status VARCHAR(50),
    verification_method VARCHAR(50),
    transaction_id VARCHAR(255),
    name_match VARCHAR(10),
    dob_match VARCHAR(10),
    face_score VARCHAR(10),
    address_match VARCHAR(10),
    error_message TEXT,
    error_code VARCHAR(100),
    verified_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Security Features

### Data Masking

- Aadhaar numbers are masked in logs and responses (e.g., "1234****5678")
- Only first 4 and last 4 digits are visible
- Full Aadhaar numbers are never stored in plain text

### Audit Trail

- All verification attempts are logged
- Complete history of verifications is maintained
- User actions are tracked with timestamps

### Error Handling

- Comprehensive error handling for API failures
- Detailed error messages for debugging
- Graceful degradation for service unavailability

## Usage Examples

### Complete OTP Verification Flow

1. **Generate Transaction ID:**
```bash
curl -X GET http://localhost:8080/api/aadhaar/transaction-id
```

2. **Generate OTP:**
```bash
curl -X POST http://localhost:8080/api/aadhaar/generate-otp \
  -H "Content-Type: application/json" \
  -d '{
    "aadhaarNumber": "123456789012",
    "transactionId": "TXN_1703123456789_abc12345"
  }'
```

3. **Verify OTP:**
```bash
curl -X POST http://localhost:8080/api/aadhaar/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "otp": "123456",
    "transactionId": "TXN_1703123456789_abc12345",
    "aadhaarNumber": "123456789012"
  }'
```

### Basic Verification

```bash
curl -X POST http://localhost:8080/api/aadhaar/verify \
  -H "Content-Type: application/json" \
  -d '{
    "aadhaarNumber": "123456789012",
    "name": "John Doe",
    "dateOfBirth": "01/01/1990",
    "gender": "MALE"
  }'
```

## Development Notes

### Demo Mode

The current implementation includes a demo mode for development purposes. In production:

1. Replace the simulated verification with actual UIDAI API calls
2. Implement proper certificate-based authentication
3. Add additional security measures as per UIDAI guidelines
4. Implement rate limiting and throttling

### Testing

For testing purposes, use the following test Aadhaar numbers:
- Valid format: `123456789012`
- Invalid format: `12345678901` (11 digits)

### Error Codes

Common error codes:
- `OTP_VERIFICATION_FAILED`: OTP verification failed
- `VERIFICATION_FAILED`: General verification failure
- `INVALID_AADHAAR_FORMAT`: Invalid Aadhaar number format

## Compliance

This implementation follows UIDAI guidelines for:
- Data masking and privacy
- Audit trail maintenance
- Error handling
- API integration patterns

## Future Enhancements

- Biometric verification support
- Face recognition integration
- Address verification
- Real-time verification status updates
- Webhook notifications
- Bulk verification support 