# Web3 Integration Template

## Principles
- Wallet signs transactions (MetaMask deep link / WalletConnect).
- Android app only builds intent/session metadata.
- Store transaction hash + status in Firestore.

## MetaMask Deep Link flow
1. Build dApp URL payload with contract call context.
2. Launch `https://metamask.app.link/dapp/{url}`.
3. User confirms transaction in wallet.
4. Capture callback / poll chain indexer for tx status.
5. Write `{txHash, status}` to `investments`.

## WalletConnect flow (alternative)
1. Start WalletConnect pairing.
2. Request `eth_sendTransaction`.
3. Receive tx hash from wallet response.
4. Persist tx hash and set status `Pending`.

## Duplicate submission prevention
- Add idempotency token for each investment attempt.
- Store token in Firestore transaction doc.
- Reject new submits while latest status is `Pending`.
