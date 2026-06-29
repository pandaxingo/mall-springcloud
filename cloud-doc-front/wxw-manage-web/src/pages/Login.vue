<template>
  <v-app class="login">
    <v-content>
      <v-container fluid fill-height>
        <v-layout align-center justify-center>
          <v-flex xs12 sm8 md4>
            <v-card class="elevation-12">
              <v-toolbar dark color="primary">
                <v-toolbar-title>商城后端管理系统</v-toolbar-title>
                <v-spacer></v-spacer>
              </v-toolbar>
              <v-card-text>
                <v-form>
                  <v-text-field prepend-icon="person" v-model="username" label="用户名" type="text" />
                  <v-text-field
                    prepend-icon="lock"
                    v-model="password"
                    label="密码"
                    id="password"
                    :append-icon="e1 ? 'visibility' : 'visibility_off'"
                    :append-icon-cb="() => (e1 = !e1)"
                    :type="e1 ? 'text' : 'password'"
                    @keyup.enter="doLogin"
                  />
                </v-form>
              </v-card-text>
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn color="primary" :loading="loading" @click="doLogin">登录</v-btn>
              </v-card-actions>
            </v-card>
          </v-flex>
        </v-layout>
      </v-container>
    </v-content>
    <v-dialog v-model="dialog" width="300px">
      <v-alert icon="warning" color="error" :value="true">
        用户名和密码不能为空
      </v-alert>
    </v-dialog>
  </v-app>
</template>

<script>
export default {
  data: () => ({
    username: "",
    password: "",
    dialog: false,
    e1: false,
    loading: false
  }),
  methods: {
    doLogin() {
      if (!this.username || !this.password) {
        this.dialog = true;
        return false;
      }

      this.loading = true;
      this.$http
        .post(
          "/auth/accredit?username=" +
            encodeURIComponent(this.username) +
            "&password=" +
            encodeURIComponent(this.password)
        )
        .then(res => {
          if (res.status === 200) {
            return this.$http.get("/auth/verify");
          }
          return Promise.reject(res);
        })
        .then(resp => {
          window.localStorage.setItem("BS_USER", JSON.stringify(resp.data));
          this.$router.push("/success");
          this.$message.success("登录成功");
        })
        .catch(() => {
          this.$message.error("用户名或密码错误");
        })
        .then(() => {
          this.loading = false;
        });
    }
  }
};
</script>

<style scoped>
.login {
  margin: 0;
  padding: 0;
  background: #fff;
}
</style>
