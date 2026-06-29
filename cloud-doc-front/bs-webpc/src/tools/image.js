import placeholder from "@/assets/image/bs2020_placeholder.png";

const API_HOST = "http://127.0.0.1:10010";

export function firstImage(value) {
  if (!value) {
    return "";
  }
  return String(value).split(",")[0].trim();
}

export function imageUrl(value) {
  const url = firstImage(value);
  if (!url) {
    return placeholder;
  }

  if (url.startsWith("data:") || url.startsWith("blob:")) {
    return url;
  }

  if (url.startsWith("/api/")) {
    return API_HOST + url;
  }

  if (url.startsWith("/images/")) {
    return API_HOST + "/api/upload" + url;
  }

  if (url.startsWith("images/")) {
    return API_HOST + "/api/upload/" + url;
  }

  if (url.indexOf("http://localhost:10010") === 0) {
    return url.replace("http://localhost:10010", API_HOST);
  }

  if (url.indexOf("http://localhost:8082/images/") === 0) {
    return url.replace("http://localhost:8082/images/", API_HOST + "/api/upload/images/");
  }

  if (/^https?:\/\//i.test(url)) {
    return url;
  }

  return API_HOST + "/api/upload/images/" + url.replace(/^\/+/, "");
}

export function imageError(event) {
  if (event && event.target && event.target.src !== placeholder) {
    event.target.src = placeholder;
  }
}
