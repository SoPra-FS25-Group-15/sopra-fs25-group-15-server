#!/usr/bin/env python3
import argparse
import requests

def extract_last_segment(path: str) -> str:
    """Helper function to extract the last segment from a URL-like path."""
    segments = path.strip("/").split("/")
    return segments[-1] if segments else ""

def main():
    # Set up command-line argument parsing.
    parser = argparse.ArgumentParser(
        description="Download radio channel MP3 from radio.garden given a city name."
    )
    parser.add_argument("city", help="City name to search (e.g. 'Zurich')")
    args = parser.parse_args()

    # 1. Make the search request.
    search_url = f"https://radio.garden/api/search/secure?q={args.city}"
    try:
        search_resp = requests.get(search_url)
        search_resp.raise_for_status()
    except requests.RequestException as e:
        print(f"Error during search request: {e}")
        return

    search_data = search_resp.json()
    hits = search_data.get("hits", {}).get("hits", [])
    if not hits:
        print(f"No search results found for city: {args.city}")
        return

    # Use the first result and extract the place id from the "url" field.
    first_result = hits[0]
    source = first_result.get("_source", {})
    url_field = source.get("url", "")
    if not url_field:
        print("The first search result does not contain a 'url' field.")
        return

    place_id = extract_last_segment(url_field)
    print(f"Found place id: {place_id}")

    # 2. Get the page details with the place id.
    page_url = f"https://radio.garden/api/ara/content/secure/page/{place_id}"
    try:
        page_resp = requests.get(page_url)
        page_resp.raise_for_status()
    except requests.RequestException as e:
        print(f"Error during page request: {e}")
        return

    page_data = page_resp.json()
    data = page_data.get("data", {})
    content = data.get("content", [])
    if not content:
        print("No content found on the page data.")
        return

    # Look for the first channel item in a list-type content.
    channel_item = None
    for content_item in content:
        if content_item.get("type") == "list" and "items" in content_item and content_item["items"]:
            channel_item = content_item["items"][0]
            break

    if channel_item is None:
        print("Could not find any channel items in the page content.")
        return

    page_info = channel_item.get("page", {})
    channel_url = page_info.get("url", "")
    if not channel_url:
        print("Channel item does not contain a 'url' field.")
        return

    channel_code = extract_last_segment(channel_url)
    print(f"Found channel code: {channel_code}")

    # 3. Download the audio file using the channel code.
    listen_url = f"https://radio.garden/api/ara/content/listen/{channel_code}/channel.mp3?111"
    print(listen_url)

if __name__ == "__main__":
    main()