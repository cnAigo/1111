export class RequestModelParser {
  static async parseCurl(curl) {
    return {
      method: 'GET',
      baseURL: '',
      path: '/',
      headers: {},
      query: {},
      body: { mode: 'none' },
    }
  }

  static toCurl(model) {
    return ''
  }
}
