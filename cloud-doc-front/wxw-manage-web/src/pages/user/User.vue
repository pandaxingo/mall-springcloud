<template>
  <v-card>
    <v-toolbar class="elevation-0">
      <v-btn color="primary" @click="addUser">新增用户</v-btn>
      <v-spacer/>
      <v-flex xs3>
        <v-text-field
          append-icon="search"
          label="搜索用户名/手机号"
          single-line
          hide-details
          v-model="search"
        />
      </v-flex>
    </v-toolbar>
    <v-divider/>
    <v-data-table
      :headers="headers"
      :items="filteredUsers"
      :items-per-page="5"
      class="elevation-1">
      <template slot="items" slot-scope="props">
        <td class="text-xs-center">{{ props.item.id }}</td>
        <td class="text-xs-left">{{ props.item.username }}</td>
        <td class="text-xs-left">{{ props.item.phone }}</td>
        <td class="text-xs-left">{{ props.item.created }}</td>
        <td class="justify-center layout px-0">
          <v-btn icon @click="editUser(props.item)">
            <i class="el-icon-edit"/>
          </v-btn>
          <v-btn icon @click="deleteUser(props.item)">
            <i class="el-icon-delete"/>
          </v-btn>
        </td>
      </template>
    </v-data-table>

    <v-dialog max-width="500" v-model="show" persistent>
      <v-card>
        <v-toolbar dense dark color="primary">
          <v-toolbar-title>{{ isEdit ? '修改' : '新增' }}用户</v-toolbar-title>
          <v-spacer/>
          <v-btn icon @click="show = false">
            <v-icon>close</v-icon>
          </v-btn>
        </v-toolbar>
        <v-card-text>
          <v-form ref="userForm" v-model="valid">
            <v-text-field
              label="用户名"
              v-model="form.username"
              :rules="[v => !!v || '请输入用户名']"
              required
            />
            <v-text-field
              label="手机号"
              v-model="form.phone"
              :rules="[v => !v || /^1[3-9]\\d{9}$/.test(v) || '手机号格式不正确']"
            />
            <v-text-field
              label="密码"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              :append-icon="showPassword ? 'visibility' : 'visibility_off'"
              :append-icon-cb="() => (showPassword = !showPassword)"
              :rules="[v => isEdit || !!v || '新增用户请输入密码']"
            />
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer/>
          <v-btn flat @click="show = false">取消</v-btn>
          <v-btn color="primary" :loading="saving" @click="saveUser">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script>
export default {
  name: "user",
  data() {
    return {
      search: "",
      headers: [
        { text: "用户编号", align: "center", value: "id" },
        { text: "用户名", sortable: false, value: "username" },
        { text: "手机号", sortable: false, value: "phone" },
        { text: "注册时间", value: "created" },
        { text: "操作", align: "center", sortable: false }
      ],
      userList: [],
      show: false,
      isEdit: false,
      valid: false,
      saving: false,
      showPassword: false,
      form: {
        id: null,
        username: "",
        phone: "",
        password: ""
      }
    };
  },
  computed: {
    filteredUsers() {
      if (!this.search) {
        return this.userList;
      }
      const keyword = this.search.trim().toLowerCase();
      return this.userList.filter(user => {
        return (
          (user.username || "").toLowerCase().indexOf(keyword) > -1 ||
          (user.phone || "").indexOf(keyword) > -1
        );
      });
    }
  },
  mounted() {
    this.getUsers();
  },
  methods: {
    getUsers() {
      this.$http
        .get("/user/list")
        .then(resp => {
          this.userList = resp.data;
        })
        .catch(() => {
          this.$message.error("获取用户列表失败！");
        });
    },
    addUser() {
      this.isEdit = false;
      this.form = { id: null, username: "", phone: "", password: "" };
      this.show = true;
    },
    editUser(user) {
      this.isEdit = true;
      this.form = {
        id: user.id,
        username: user.username,
        phone: user.phone,
        password: ""
      };
      this.show = true;
    },
    saveUser() {
      if (!this.$refs.userForm.validate()) {
        return;
      }
      this.saving = true;
      const request = this.isEdit
        ? this.$http.put("/user/" + this.form.id, this.form)
        : this.$http.post("/user", this.form);
      request
        .then(() => {
          this.$message.success("保存成功！");
          this.show = false;
          this.getUsers();
        })
        .catch(() => {
          this.$message.error("保存失败！");
        })
        .then(() => {
          this.saving = false;
        });
    },
    deleteUser(user) {
      this.$message.confirm("确认删除该用户吗？").then(() => {
        this.$http
          .delete("/user/" + user.id)
          .then(() => {
            this.$message.success("删除成功！");
            this.getUsers();
          })
          .catch(() => {
            this.$message.error("删除失败！");
          });
      });
    }
  }
};
</script>

<style scoped>
</style>
