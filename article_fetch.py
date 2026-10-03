"""Download one news URL and return its main article body as plain text."""

from __future__ import annotations

from html import unescape
from html.parser import HTMLParser
from urllib.parse import urlsplit


class _TitleParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.meta_title = ""
        self.page_title = ""
        self._inside_title = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = dict(attrs)
        if tag == "meta" and attributes.get("property") == "og:title":
            self.meta_title = unescape(attributes.get("content") or "").strip()
        elif tag == "title":
            self._inside_title = True

    def handle_endtag(self, tag: str) -> None:
        if tag == "title":
            self._inside_title = False

    def handle_data(self, data: str) -> None:
        if self._inside_title:
            self.page_title += data


def fetch_article(url: str) -> dict[str, str]:
    """Return a news page's title and main body from one HTTP(S) request."""
    if not isinstance(url, str) or not url.strip():
        raise ValueError("URL must be a non-empty string")
    url = url.strip()
    parsed = urlsplit(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise ValueError("URL must be a valid http:// or https:// address")

    try:
        from trafilatura import extract, fetch_url
    except ImportError as exc:
        raise RuntimeError("Install the article extractor: pip install trafilatura") from exc

    html = fetch_url(url)
    if not html:
        raise RuntimeError("Could not download the article page")
    body = extract(html, url=url, include_comments=False)
    if not body or not body.strip():
        raise RuntimeError("Could not find article text on this page")
    parser = _TitleParser()
    parser.feed(html)
    title = parser.meta_title or unescape(parser.page_title).strip()
    return {"url": url, "title": title, "body": body.strip()}


def fetch_article_body(url: str) -> str:
    """Return only article text for existing callers."""
    return fetch_article(url)["body"]


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Print the body of one news URL")
    parser.add_argument("url", help="Article URL")
    args = parser.parse_args()
    print(fetch_article_body(args.url))
