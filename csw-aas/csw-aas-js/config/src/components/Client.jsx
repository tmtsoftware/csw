import request from 'superagent'

export const post = (url, callback, input = '') => {
  request
    .post(url)
    .set('Content-Type', 'application/json')
    .send(input)
    .then(res => {
      callback(res.text)
    }, err => {
      callback(err.response)
    }
    )
}

export const sPost = (url, callback, token, input = '') => {
  request
    .post(url)
    .set('Content-Type', 'text/plain')
    .set('Authorization', `Bearer ${token()}`)
    .send(input)
    .then(res => {
      callback(res.text)
    }, err => {
      callback(err.toString())
    }
    )
}

export const get = (url, callback) => {
  request
    .get(url)
    .set('Content-Type', 'application/json')
    .send()
    .then(res => {
      console.log(res.text)
      if (res.body) {
        callback(res.text)
      }
    }, err => console.log(err)

    )
}
