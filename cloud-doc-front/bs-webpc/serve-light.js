process.env.NODE_ENV = 'development';

const fs = require('fs');
const logFile = 'serve-light.log';

function log(message) {
  fs.appendFileSync(logFile, message + '\n');
  console.log(message);
}

const Service = require('@vue/cli-service/lib/Service');
const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const portfinder = require('portfinder');

log('init');
const service = new Service(process.cwd());
service.init('development');

log('resolve config');
const config = service.resolveWebpackConfig();
config.plugins = config.plugins || [];
config.plugins.push(new webpack.HotModuleReplacementPlugin());

log('compiler');
const compiler = webpack(config);

compiler.hooks.done.tap('serve-light', stats => {
  log(stats.hasErrors() ? stats.toString({ colors: false }) : 'compiled');
});

portfinder.basePort = process.env.PORT || 9002;
portfinder.getPort((portErr, port) => {
  if (portErr) {
    log(portErr.stack || portErr.message);
    process.exit(1);
  }

  log('server construct');
  const server = new WebpackDevServer(compiler, {
    host: 'localhost',
    port,
    publicPath: '/',
    contentBase: 'public',
    historyApiFallback: true,
    hot: true,
    overlay: { warnings: false, errors: true },
    stats: 'errors-only',
    clientLogLevel: 'warning'
  });

  log('listen');
  server.listen(port, 'localhost', err => {
    if (err) {
      log(err.stack || err.message);
      process.exit(1);
    }
    log('listening http://localhost:' + port);
    if (port !== 9002) {
      log('port 9002 is in use, switched to ' + port);
    }
  });
});
