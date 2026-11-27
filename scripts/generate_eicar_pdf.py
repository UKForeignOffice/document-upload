from pathlib import Path
import tempfile

import pikepdf

EICAR_SIGNATURE = b"X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"

OUTPUT_FILE = "eicar_embedded_test.pdf"
ATTACHMENT_NAME = "eicar.com"

pdf = pikepdf.new()
pdf.add_blank_page(page_size=(612, 792))
script_dir = Path(__file__).resolve().parent

tmp_path = None
try:
    with tempfile.NamedTemporaryFile(delete=False, suffix=".com", dir=script_dir) as tmp:
        tmp.write(EICAR_SIGNATURE)
        tmp_path = Path(tmp.name)

    file_spec = pikepdf.AttachedFileSpec.from_filepath(
        pdf,
        tmp_path,
        description="EICAR antivirus test signature",
    )
    pdf.attachments[ATTACHMENT_NAME] = file_spec

    js = "app.alert('EICAR embedded PDF test file loaded.');"
    pdf.Root.OpenAction = pikepdf.Dictionary(
        Type=pikepdf.Name("/Action"),
        S=pikepdf.Name("/JavaScript"),
        JS=pikepdf.String(js),
    )

    pdf.save(OUTPUT_FILE)
    print(f"Created {OUTPUT_FILE} with embedded attachment {ATTACHMENT_NAME}")
finally:
    if tmp_path is not None:
        tmp_path.unlink(missing_ok=True)
