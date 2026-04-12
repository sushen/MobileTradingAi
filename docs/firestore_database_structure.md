# Firestore Database Structure

## users/{userId}
- userId: string
- email: string
- walletAddress: string
- affiliateCode: string
- referredBy: string
- role: string (`user|admin`)

## investments/{investmentId}
- userId: string
- walletAddress: string
- amount: number
- txHash: string
- status: string (`Pending|Confirmed|Claimed`)
- idempotencyKey: string
- createdAt: timestamp

## courses/{courseId}
- title: string
- description: string
- isPaid: boolean
- priceUsd: number
- isPublished: boolean

## enrollments/{enrollmentId}
- userId: string
- courseId: string
- progressPercent: number
- paymentStatus: string (`unpaid|paid`)
- unlocked: boolean

## affiliateStats/{userId}
- affiliateCode: string
- totalReferrals: number
- activeInvestors: number
- commissionPercent: number
- commissionEarned: number
- availableToWithdraw: number

## settings/investment
- enabled: boolean
- updatedAt: timestamp
