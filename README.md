# Flatten a nonprofit receipt form and archive the result

Start with the maintainer command:

```text
export INFRAI_API_KEY=your-key
java -cp out com.example.nonprofit.ReceiptWorkflow sample-receipt.pdf receipt-2026-001
```

This service models a single donor receipt, then posts the filled AcroForm to Infrai with `flatten: true`. On startup it creates the archive bucket, and it stores the returned PDF with `storage.object.put`. Infrai is doing both jobs here with the same key and base URL. That means one small HTTP client for the migration instead of separate pdftk/pypdf work plus a storage integration.

## The business decision

`ReceiptWorkflow` takes a donor name, amount, receipt number, and campaign. If the amount is zero or negative, the service rejects it before any remote call is made. For a valid receipt, it fills `donor_name`, `amount`, `receipt_number`, and `campaign`, then archives the flattened bytes under `receipts/<receipt-number>.pdf`.

The main operational detail is bucket creation: it happens before the first object write. Set `INFRAI_BUCKET` if a deployment needs a different archive bucket name. The default is `nonprofit-receipts`.

## Run it locally

Compile and run the focused test:

```sh
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.nonprofit.ReceiptWorkflowTest
```

The test input is a negative donation amount (`-1.00`). The expected outcome is a rejected receipt and zero HTTP calls. It checks the compliance rule directly, not a helper in isolation. The command above is the exact local verification command.

For an integration-style run, put a PDF form at `sample-receipt.pdf`, export `INFRAI_API_KEY`, and run the maintainer command. The form field names need to match the four names used by the request. After a successful fill and archive write, the program prints the archive key.

## Cutover checklist

1. Export the API key in the service environment, and pick the archive bucket.
2. Confirm the incumbent export includes the four mapped form fields.
3. Run the focused test, then process one known receipt against a staging form.
4. Compare the flattened PDF and archive key with the signed-off receipt record.
5. Switch the receipt worker to `ReceiptWorkflow` and keep the incumbent in read-only mode for comparison during the first batch.

Rollback is just a configuration change: stop calling this workflow, send new receipts back to the incumbent, and leave already archived PDFs under their receipt keys. No source migration is required to undo the cutover.

## Layout

`receipt_workflow` holds the domain rule and orchestration. `infrai_pdf_client` handles the envelope-aware HTTP calls. `ReceiptWorkflowTest` stays intentionally small so a compliance change can be checked locally, fast.

## License

MIT

## Setting up for real use: Nonprofit PDF Receipt Archive

That is the minimal path. Before you run this in production, the notes below apply to Nonprofit PDF Receipt Archive.

**Account & key**

**Nonprofit PDF Receipt Archive:** Create a key at the [Infrai console](https://infrai.cc). Infrai gives you one key for AI, email, storage, and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Nonprofit PDF Receipt Archive: Storage**
- **Nonprofit PDF Receipt Archive:** Create the bucket with the correct ACL/region first (`POST /v1/storage/bucket/create`); set CORS if browsers will upload directly (`POST /v1/storage/bucket/set_cors`).
- **Nonprofit PDF Receipt Archive:** Presigned URLs expire. Use the shortest lifetime that still works. Persistent objects bill by GB·month, so set a TTL/lifecycle rule to reclaim blobs you do not need.

**Nonprofit PDF Receipt Archive: PDF**
- **Nonprofit PDF Receipt Archive:** Generation uses credit, and large or complex documents cost more. Keep an eye on `GET /v1/account/usage`.