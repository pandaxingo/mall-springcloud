<template>
  <v-card>
    <v-toolbar class="elevation-0">
      <v-btn color="primary" @click="addGoods">新增商品</v-btn>
      <v-spacer/>
      <v-flex xs3>
        状态：
        <v-btn-toggle v-model.lazy="filter.saleable">
          <v-btn flat :value="null">全部</v-btn>
          <v-btn flat :value="true">上架</v-btn>
          <v-btn flat :value="false">下架</v-btn>
        </v-btn-toggle>
      </v-flex>
      <v-flex xs3>
        <v-text-field
          append-icon="search"
          label="搜索"
          single-line
          hide-details
          v-model="filter.search"
        />
      </v-flex>
    </v-toolbar>
    <v-divider/>
    <v-data-table
      :headers="headers"
      :items="goodsList"
      :pagination.sync="pagination"
      :total-items="totalGoods"
      :loading="loading"
      class="elevation-1">
      <template slot="items" slot-scope="props">
        <td class="text-xs-center">{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.title }}</td>
        <td class="text-xs-center">{{ props.item.cname }}</td>
        <td class="text-xs-center">{{ props.item.bname }}</td>
        <td class="justify-center layout px-0">
          <v-btn icon @click="editGoods(props.item)">
            <i class="el-icon-edit"/>
          </v-btn>
          <v-btn icon @click="deleteGoods(props.item)">
            <i class="el-icon-delete"/>
          </v-btn>
          <v-btn flat small color="warning" v-if="props.item.saleable" @click="updateSaleable(props.item, false)">下架</v-btn>
          <v-btn flat small color="success" v-else @click="updateSaleable(props.item, true)">上架</v-btn>
        </td>
      </template>
    </v-data-table>

    <v-dialog max-width="800" v-model="show" persistent scrollable>
      <v-card>
        <v-toolbar dense dark color="primary">
          <v-toolbar-title>{{ isEdit ? '修改' : '新增' }}商品</v-toolbar-title>
          <v-spacer/>
          <v-btn icon @click="closeWindow">
            <v-icon>close</v-icon>
          </v-btn>
        </v-toolbar>
        <v-card-text class="px-3" style="height: 600px">
          <goods-form :oldGoods="oldGoods" :step="step" @close="closeWindow" :is-edit="isEdit" ref="goodsForm"/>
        </v-card-text>
        <v-card-actions class="elevation-10">
          <v-flex class="xs3 mx-auto">
            <v-btn @click="previous" color="primary" :disabled="step === 1">上一步</v-btn>
            <v-btn @click="next" color="primary" :disabled="step === 4">下一步</v-btn>
          </v-flex>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script>
  import GoodsForm from './GoodsForm'

  export default {
    name: 'goods',
    data() {
      return {
        filter: {
          saleable: null,
          search: ''
        },
        totalGoods: 0,
        goodsList: [],
        loading: false,
        pagination: {},
        headers: [
          {text: '商品编号', align: 'center', value: 'id'},
          {text: '标题', align: 'center', sortable: false, value: 'title'},
          {text: '商品分类', align: 'center', sortable: false, value: 'cname'},
          {text: '品牌', align: 'center', value: 'bname', sortable: false},
          {text: '操作', align: 'center', sortable: false}
        ],
        show: false,
        oldGoods: {},
        isEdit: false,
        step: 1
      }
    },
    mounted() {
      this.getDataFromServer();
    },
    watch: {
      pagination: {
        deep: true,
        handler() {
          this.getDataFromServer();
        }
      },
      filter: {
        deep: true,
        handler() {
          this.pagination.page = 1;
          this.getDataFromServer();
        }
      }
    },
    methods: {
      getDataFromServer() {
        this.loading = true;
        this.$http.get('/item/spu/page', {
          params: {
            key: this.filter.search,
            page: this.pagination.page,
            rows: this.pagination.rowsPerPage,
            saleable: this.filter.saleable
          }
        }).then(resp => {
          const data = resp.data || {};
          this.goodsList = data.items || [];
          this.totalGoods = data.total || 0;
        }).catch(() => {
          this.goodsList = [];
          this.totalGoods = 0;
        }).finally(() => {
          this.loading = false;
        });
      },
      addGoods() {
        this.isEdit = false;
        this.show = true;
        this.oldGoods = {};
        this.step = 1;
      },
      async editGoods(oldGoods) {
        oldGoods.spuDetail = await this.$http.loadData('/item/spu/detail/' + oldGoods.id);
        oldGoods.skus = await this.$http.loadData('/item/sku/list?id=' + oldGoods.id);
        this.isEdit = true;
        this.show = true;
        this.oldGoods = oldGoods;
        this.step = 1;
      },
      deleteGoods(goods) {
        this.$message.confirm('确认删除该商品吗？')
          .then(() => this.$http.delete('/item/goods/' + goods.id))
          .then(() => {
            this.$message.success('删除成功！');
            this.getDataFromServer();
          })
          .catch(() => {});
      },
      updateSaleable(goods, saleable) {
        const text = saleable ? '上架' : '下架';
        this.$http
          .put('/item/goods/' + goods.id + '/saleable/' + saleable)
          .then(() => {
            this.$message.success(text + '成功！');
            this.getDataFromServer();
          })
          .catch(() => {
            this.$message.error(text + '失败！');
          });
      },
      closeWindow() {
        this.getDataFromServer();
        this.show = false;
        this.step = 1;
      },
      previous() {
        if (this.step > 1) {
          this.step--;
        }
      },
      next() {
        if (this.step < 4 && this.$refs.goodsForm.$refs.basic.validate()) {
          this.step++;
        }
      }
    },
    components: {
      GoodsForm
    }
  }
</script>

<style scoped>
</style>
