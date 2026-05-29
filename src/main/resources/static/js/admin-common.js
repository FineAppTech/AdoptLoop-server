// 관리자 페이지 공통: Admin Key 인증 fetch + 미로그인 가드.
const ADMIN_KEY_HEADER = 'X-Admin-Key';

function adminKey() {
  return localStorage.getItem('admin_key');
}

function authFetch(url, opts = {}) {
  opts.headers = Object.assign({}, opts.headers, { [ADMIN_KEY_HEADER]: adminKey() });
  if (opts.body && typeof opts.body !== 'string') {
    opts.body = JSON.stringify(opts.body);
    opts.headers['Content-Type'] = 'application/json';
  }
  return fetch(url, opts).then(r => {
    if (r.status === 401) { location.href = '/admin/login'; throw new Error('unauthorized'); }
    return r;
  });
}

// 키가 없으면 로그인 페이지로 돌려보낸다.
if (!adminKey() && location.pathname !== '/admin/login') {
  location.href = '/admin/login';
}
