<template>
  <div>
    <el-card>
      <div slot="header">
        <span class="panel-title home-title">{{ $t('m.Team_Award') }}</span>
        <div class="filter-row">
          <span class="filter-item">
            <span style="margin-right:8px">{{ $t('m.Team_Award_Page_Size') }}</span>
            <el-input-number
              v-model="teamAwardPageSize"
              :min="1"
              :max="50"
              size="small"
            ></el-input-number>
            <el-button
              type="primary"
              size="small"
              style="margin-left:8px"
              :loading="savingPageSize"
              @click="savePageSize"
            >
              {{ $t('m.Save') }}
            </el-button>
          </span>

          <span class="filter-item">
            <el-button
              type="primary"
              size="small"
              icon="el-icon-plus"
              @click="openDialog('add', null)"
            >
              {{ $t('m.Add_Team_Award') }}
            </el-button>
          </span>

          <span class="filter-item">
            <vxe-input
              v-model="keyword"
              :placeholder="$t('m.Enter_keyword')"
              type="search"
              size="medium"
              @search-click="filterByKeyword"
              @keyup.enter.native="filterByKeyword"
            ></vxe-input>
          </span>
        </div>
      </div>

      <vxe-table
        :loading="loading"
        :data="list"
        auto-resize
        stripe
        align="center"
      >
        <vxe-table-column field="id" width="80" title="ID"></vxe-table-column>
        <vxe-table-column field="title" min-width="160" :title="$t('m.Title')" show-overflow>
        </vxe-table-column>
        <vxe-table-column field="contestName" min-width="160" :title="$t('m.Contest_Name')" show-overflow>
        </vxe-table-column>
        <vxe-table-column field="award" min-width="140" :title="$t('m.Award')" show-overflow>
        </vxe-table-column>
        <vxe-table-column min-width="130" :title="$t('m.Award_Time')">
          <template v-slot="{ row }">
            <span v-if="row.awardTime">{{ row.awardTime | localtime('YYYY-MM-DD') }}</span>
            <span v-else>-</span>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="120" :title="$t('m.Visible')">
          <template v-slot="{ row }">
            <el-tag v-if="row.status === 0" type="success">{{ $t('m.Visible') }}</el-tag>
            <el-tag v-else type="info">{{ $t('m.Hidden') }}</el-tag>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="140" :title="$t('m.Photo')">
          <template v-slot="{ row }">
            <a v-if="row.photo" :href="row.photo" target="_blank" rel="noopener">
              <img class="team-award-photo" :src="row.photo" alt="photo" />
            </a>
            <span v-else>-</span>
          </template>
        </vxe-table-column>
        <vxe-table-column min-width="120" :title="$t('m.Option')">
          <template v-slot="{ row }">
            <el-tooltip effect="dark" :content="$t('m.Edit')" placement="top">
              <el-button
                icon="el-icon-edit"
                size="mini"
                type="primary"
                @click.native="openDialog('update', row)"
              ></el-button>
            </el-tooltip>
            <el-tooltip effect="dark" :content="$t('m.Delete')" placement="top">
              <el-button
                icon="el-icon-delete"
                size="mini"
                type="danger"
                @click.native="deleteRow(row)"
              ></el-button>
            </el-tooltip>
          </template>
        </vxe-table-column>
      </vxe-table>

      <div class="panel-options">
        <el-pagination
          class="page"
          layout="prev, pager, next"
          @current-change="currentChange"
          :page-size="limit"
          :current-page.sync="currentPage"
          :total="total"
        ></el-pagination>
      </div>
    </el-card>

    <el-dialog
      :title="$t('m.' + dialogTitleKey)"
      width="720px"
      :visible.sync="dialogVisible"
      :close-on-click-modal="false"
    >
      <el-form
        ref="awardForm"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-row :gutter="20">
          <el-col :md="12" :xs="24">
            <el-form-item :label="$t('m.Title')" prop="title" required>
              <el-input v-model="form.title" size="small"></el-input>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item :label="$t('m.Contest_Name')" prop="contestName" required>
              <el-input v-model="form.contestName" size="small"></el-input>
            </el-form-item>
          </el-col>

          <el-col :md="12" :xs="24">
            <el-form-item :label="$t('m.Award')" prop="award" required>
              <el-input v-model="form.award" size="small"></el-input>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item :label="$t('m.Award_Time')" prop="awardTime" required>
              <el-date-picker
                v-model="form.awardTime"
                type="date"
                :placeholder="$t('m.Award_Time')"
                style="width: 100%"
              ></el-date-picker>
            </el-form-item>
          </el-col>

          <el-col :md="12" :xs="24">
            <el-form-item :label="$t('m.Visible')" prop="status" required>
              <el-switch
                v-model="visibleSwitch"
                :active-text="$t('m.Visible')"
                :inactive-text="$t('m.Hidden')"
              ></el-switch>
            </el-form-item>
          </el-col>

          <el-col :md="12" :xs="24">
            <el-form-item :label="$t('m.Photo')">
              <div class="photo-row">
                <a v-if="form.photo" :href="form.photo" target="_blank" rel="noopener">
                  <img class="team-award-photo-large" :src="form.photo" alt="photo" />
                </a>
                <span v-else class="photo-empty">-</span>
                <el-upload
                  class="upload"
                  action=""
                  :show-file-list="false"
                  :http-request="uploadPhoto"
                  :before-upload="beforeUpload"
                >
                  <el-button size="small" type="primary" :loading="uploading">
                    {{ $t('m.Upload') }}
                  </el-button>
                </el-upload>
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item :label="$t('m.Team_Award_Description')">
              <el-input
                type="textarea"
                :rows="4"
                v-model="form.description"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item style="text-align:center">
          <el-button
            type="primary"
            :loading="saving"
            @click="submit"
          >
            {{ $t('m.' + dialogBtnKey) }}
          </el-button>
          <el-button @click="dialogVisible = false">{{ $t('m.Cancel') }}</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script>
import api from '@/common/api';
import myMessage from '@/common/message';

export default {
  name: 'TeamAward',
  data() {
    return {
      loading: false,
      saving: false,
      uploading: false,
      savingPageSize: false,

      keyword: '',
      currentPage: 1,
      limit: 10,
      total: 0,
      list: [],

      teamAwardPageSize: 6,

      dialogVisible: false,
      dialogMode: 'add',
      form: {
        id: null,
        title: '',
        contestName: '',
        award: '',
        awardTime: null,
        photo: '',
        description: '',
        status: 0,
      },
      rules: {
        title: [{ required: true, message: 'required', trigger: 'blur' }],
        contestName: [{ required: true, message: 'required', trigger: 'blur' }],
        award: [{ required: true, message: 'required', trigger: 'blur' }],
        awardTime: [{ required: true, message: 'required', trigger: 'change' }],
      },
    };
  },
  computed: {
    dialogTitleKey() {
      return this.dialogMode === 'add' ? 'Add_Team_Award' : 'Update_Team_Award';
    },
    dialogBtnKey() {
      return this.dialogMode === 'add' ? 'To_Add' : 'To_Update';
    },
    visibleSwitch: {
      get() {
        return this.form.status === 0;
      },
      set(val) {
        this.form.status = val ? 0 : 1;
      },
    },
  },
  mounted() {
    this.getPageSize();
    this.getList(1);
  },
  methods: {
    currentChange(page) {
      this.currentPage = page;
      this.getList(page);
    },
    filterByKeyword() {
      this.getList(1);
    },
    getList(page) {
      this.loading = true;
      api.admin_getTeamAwardList(page, this.limit, this.keyword).then(
        (res) => {
          this.loading = false;
          const data = res.data.data || {};
          this.currentPage = data.current || page;
          this.total = data.total || 0;
          this.list = data.records || [];
        },
        () => {
          this.loading = false;
        }
      );
    },
    getPageSize() {
      api.admin_getTeamAwardPageSize().then(
        (res) => {
          this.teamAwardPageSize = res.data.data;
        },
        () => {}
      );
    },
    savePageSize() {
      this.savingPageSize = true;
      api.admin_setTeamAwardPageSize(this.teamAwardPageSize).then(
        () => {
          this.savingPageSize = false;
          myMessage.success(this.$i18n.t('m.Update_Successfully'));
        },
        () => {
          this.savingPageSize = false;
        }
      );
    },
    openDialog(mode, row) {
      this.dialogMode = mode;
      if (mode === 'add') {
        this.form = {
          id: null,
          title: '',
          contestName: '',
          award: '',
          awardTime: null,
          photo: '',
          description: '',
          status: 0,
        };
      } else {
        this.form = Object.assign({}, row);
      }
      this.dialogVisible = true;
      this.$nextTick(() => {
        if (this.$refs.awardForm) {
          this.$refs.awardForm.clearValidate();
        }
      });
    },
    submit() {
      this.$refs.awardForm.validate((valid) => {
        if (!valid) {
          myMessage.warning(this.$i18n.t('m.Error_Please_check_your_choice'));
          return;
        }
        this.saving = true;
        const req =
          this.dialogMode === 'add'
            ? api.admin_addTeamAward(this.form)
            : api.admin_updateTeamAward(this.form);
        req.then(
          () => {
            this.saving = false;
            myMessage.success(
              this.dialogMode === 'add'
                ? this.$i18n.t('m.Create_Successfully')
                : this.$i18n.t('m.Update_Successfully')
            );
            this.dialogVisible = false;
            this.getList(1);
          },
          () => {
            this.saving = false;
          }
        );
      });
    },
    deleteRow(row) {
      this.$confirm(this.$i18n.t('m.Delete') + ' ?', 'Tips', {
        confirmButtonText: this.$i18n.t('m.OK'),
        cancelButtonText: this.$i18n.t('m.Cancel'),
        type: 'warning',
      }).then(() => {
        api.admin_deleteTeamAward(row.id).then(
          () => {
            myMessage.success(this.$i18n.t('m.Delete_successfully'));
            this.getList(1);
          },
          () => {}
        );
      });
    },
    beforeUpload(file) {
      const isImg = /\.(gif|jpg|jpeg|png|bmp|webp|GIF|JPG|PNG|WEBP|svg|SVG|jfif|JFIF)$/.test(
        file.name
      );
      if (!isImg) {
        this.$notify.warning({
          title: this.$i18n.t('m.File_type_not_support'),
          message: file.name + this.$i18n.t('m.is_incorrect_format_file'),
        });
        return false;
      }
      if (file.size > 5 * 1024 * 1024) {
        this.$notify.warning({
          title: this.$i18n.t('m.Exceed_max_size_limit'),
          message: file.name + this.$i18n.t('m.File_Exceed_Tips'),
        });
        return false;
      }
      return true;
    },
    uploadPhoto(req) {
      const file = req.file;
      const form = new window.FormData();
      form.append('file', file);
      this.uploading = true;
      this.$http({
        method: 'post',
        url: '/api/file/upload-team-award-img',
        data: form,
        headers: { 'content-type': 'multipart/form-data' },
      }).then(
        (res) => {
          this.uploading = false;
          const data = (res.data && res.data.data) || {};
          if (data.url) {
            this.form.photo = data.url;
            myMessage.success(this.$i18n.t('m.Upload_Successfully'));
          }
        },
        () => {
          this.uploading = false;
        }
      );
    },
  },
};
</script>

<style scoped>
.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  align-items: center;
}
.filter-item {
  display: inline-flex;
  align-items: center;
}
.team-award-photo {
  width: 70px;
  height: 42px;
  object-fit: cover;
  border-radius: 4px;
}
.team-award-photo-large {
  width: 140px;
  height: 84px;
  object-fit: cover;
  border-radius: 4px;
  margin-right: 12px;
}
.photo-row {
  display: flex;
  align-items: center;
}
.photo-empty {
  display: inline-block;
  width: 140px;
  margin-right: 12px;
  text-align: center;
}
</style>
