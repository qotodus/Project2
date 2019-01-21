const grpc = require('grpc')
const crypto = require('crypto');
const proto = grpc.load('./gigagenieRPC.proto').kt.gigagenie.ai.speech;
const fs = require('fs');
const dateFormat = require('dateformat');
const sslCred = grpc.credentials.createSsl(fs.readFileSync('./ca-bundle.pem'));
const client_id = 'Y2xpZW50X2lkMTU0NzUxMTY3MDIzNg==';
const client_key = 'Y2xpZW50X2tleTE1NDc1MTE2NzAyMzY=';
const client_secret = 'Y2xpZW50X3NlY3JldDE1NDc1MTE2Nz222222yMzY=';

function getTimeStamp() {
  return dateFormat(new Date(), 'yyyymmddHHmmssL');
};

function createSignature(id, timestamp, secret) {
  return crypto.createHmac('sha256', secret).update(id + ':' + timestamp).digest('hex');
};

function generateMetadata(params, callback) {
  const metadata = new grpc.Metadata();
  const timeStamp = getTimeStamp();
  metadata.add('x-auth-clientkey', client_key);
  metadata.add('x-auth-timestamp', timeStamp);
  const signature = createSignature(client_id, timeStamp, client_secret);
  metadata.add('x-auth-signature', signature);;
  callback(null, metadata);
};

const authCred = grpc.credentials.createFromMetadataGenerator(generateMetadata);
const credentials = grpc.credentials.combineChannelCredentials(sslCred, authCred);
const client = new proto.Gigagenie('gate.gigagenie.ai:4080', credentials);

client.getText2VoiceUrl({
  lang: 0,
  mode: 0,
  text: '방가방가.'
}, (err, msg) => {
  console.log('err:' + JSON.stringify(err) + ' msg:' + JSON.stringify(msg));
})
