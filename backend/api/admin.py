from django.contrib import admin
from django.utils.html import format_html
from django.utils.safestring import mark_safe
from django import forms
from .models import Category, Image, Album, AlbumImage, Comic, ComicChapter, Video, Profile, Note, CloudFile
import os
import uuid
import logging

logger = logging.getLogger(__name__)


class CategoryAdmin(admin.ModelAdmin):
    """标签管理后台 - 显示所有标签，管理员可增删改"""
    list_display = ('name', 'slug', 'parent', 'permission_level')
    list_filter = ('permission_level', 'parent')
    search_fields = ('name', 'slug')
    prepopulated_fields = {'slug': ('name',)}
    raw_id_fields = ('parent',)
    ordering = ['name']

    def get_queryset(self, request):
        """返回所有标签，不做过滤"""
        return super().get_queryset(request)


@admin.register(Image)
class ImageAdmin(admin.ModelAdmin):
    list_display = ('title', 'resource_id', 'resource_type', 'album_id', 'uploader', 'created_at')
    list_filter = ('resource_type', 'categories')
    search_fields = ('title', 'resource_id')
    readonly_fields = ('resource_id', 'created_at', 'updated_at')
    filter_horizontal = ('categories',)
    change_list_template = 'admin/image/change_list.html'
    fieldsets = (
        ('文件上传', {
            'fields': ('image_file', 'resource_type', 'album_id'),
            'description': '选择要上传的图片文件'
        }),
        ('基本信息', {
            'fields': ('title', 'description', 'uploader'),
            'description': '设置图片的标题和描述'
        }),
        ('标签关联', {
            'fields': ('categories',),
            'description': '为图片选择一个或多个标签'
        }),
        ('系统信息', {
            'classes': ('collapse',),
            'fields': ('resource_id', 'created_at', 'updated_at')
        })
    )

    def get_urls(self):
        """添加批量上传 URL"""
        from django.urls import path
        return [
            path('batch-upload/', self.admin_site.admin_view(self.batch_upload_view), name='image_batch_upload'),
        ] + super().get_urls()

    def batch_upload_view(self, request):
        """批量上传页面"""
        categories = Category.objects.all()
        context = {
            **self.admin_site.each_context(request),
            'categories': categories,
            'title': '批量上传图片',
            'opts': self.model._meta,
            'batch_upload_script': self._get_batch_upload_script(),
        }
        from django.shortcuts import render
        return render(request, 'admin/image/batch_upload.html', context)

    def _get_batch_upload_script(self):
        """生成批量上传页面的 JavaScript"""
        return '''
        <script>
        // ===== 并发池 =====
        async function asyncPool(promises, limit) {
            const ret = [];
            const executing = [];
            for (const p of promises) {
                const e = Promise.resolve().then(p);
                ret.push(e);
                if (limit <= promises.length) {
                    const ex = e.then(() => executing.splice(executing.indexOf(ex), 1));
                    executing.push(ex);
                    if (executing.length >= limit) {
                        await Promise.race(executing);
                    }
                }
            }
            return Promise.all(ret);
        }

        // ===== 上传单个批次 =====
        function uploadBatch(formData, csrftoken) {
            return new Promise((resolve, reject) => {
                const xhr = new XMLHttpRequest();
                xhr.open('POST', '/api/admin/images/batch-upload/');
                xhr.setRequestHeader('X-CSRFToken', csrftoken);
                xhr.onload = () => {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        try { resolve(JSON.parse(xhr.responseText)); }
                        catch(e) { resolve({success: true}); }
                    } else {
                        reject(new Error('HTTP ' + xhr.status));
                    }
                };
                xhr.onerror = () => reject(new Error('网络错误'));
                xhr.send(formData);
            });
        }

        // ===== 主上传函数 =====
        async function startBatchUpload() {
            const fileInput = document.getElementById('file_input');
            if (!fileInput.files.length) {
                alert('请先选择图片');
                return;
            }

            const files = Array.from(fileInput.files);
            const selectedCategories = Array.from(
                document.querySelectorAll('input[name="category_checkbox"]:checked')
            ).map(cb => cb.value);
            const titlePrefix = document.getElementById('title_prefix').value || '图片';

            const progressDiv = document.getElementById('upload_progress');
            const resultDiv = document.getElementById('upload_result');
            progressDiv.style.display = 'block';
            resultDiv.innerHTML = '';

            const BATCH_SIZE = 5;
            const CONCURRENT = 3;
            const csrftoken = getCookie('csrftoken');

            // 分批
            const batches = [];
            for (let i = 0; i < files.length; i += BATCH_SIZE) {
                batches.push(files.slice(i, i + BATCH_SIZE));
            }

            const totalBatches = batches.length;
            let completedBatches = 0;
            let totalUploaded = 0;
            let totalFailed = 0;

            progressDiv.innerHTML =
                '<div style="text-align:center;color:#28a745;font-size:13px;margin-bottom:8px;">📤 并行上传 ' + totalBatches + ' 批 / 共 ' + files.length + ' 张</div>' +
                '<div style="background:#e9ecef;border-radius:4px;height:10px;overflow:hidden;"><div id="upload_bar" style="background:#28a745;height:100%;width:0%;transition:width 0.3s;"></div></div>' +
                '<p id="upload_text" style="margin:8px 0 0;font-size:12px;color:#666;text-align:center;">准备上传...</p>';

            // 构建并发任务
            const batchTasks = batches.map((batch) => {
                return async () => {
                    const formData = new FormData();
                    batch.forEach((file) => {
                        formData.append('images', file, file.name);
                    });
                    formData.append('title_prefix', titlePrefix);
                    selectedCategories.forEach(catId => {
                        formData.append('categories', catId);
                    });
                    try {
                        const result = await uploadBatch(formData, csrftoken);
                        totalUploaded += result.uploaded_count || batch.length;
                    } catch (e) {
                        totalFailed += batch.length;
                    }
                    completedBatches++;
                    const pct = Math.round(completedBatches / totalBatches * 100);
                    document.getElementById('upload_bar').style.width = pct + '%';
                    document.getElementById('upload_text').textContent =
                        '上传中 ' + completedBatches + '/' + totalBatches + ' 批 | ✅' + totalUploaded + ' ❌' + totalFailed;
                };
            });

            try {
                await asyncPool(batchTasks, CONCURRENT);
                progressDiv.style.display = 'none';
                const hasFail = totalFailed > 0;
                resultDiv.innerHTML =
                    '<div style="padding:15px;background:' + (hasFail ? '#fff3cd;color:#856404;' : '#d4edda;color:#155724;') +
                    'border-radius:6px;font-size:14px;">' +
                    '🎉 上传完成！<br>' +
                    '✅ 成功: ' + totalUploaded + ' 张<br>' +
                    (hasFail ? '❌ 失败: ' + totalFailed + ' 张' : '') +
                    (selectedCategories.length > 0 ? '🏷️ 标签: ' + selectedCategories.length + ' 个' : '') +
                    '</div>';
            } catch (e) {
                progressDiv.style.display = 'none';
                resultDiv.innerHTML = '<div style="padding:10px;background:#f8d7da;color:#721c24;border-radius:4px;">❌ 上传失败: ' + e.message + '</div>';
            }
        }

        function getCookie(name) {
            var cookieValue = null;
            if (document.cookie && document.cookie !== '') {
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {
                    var cookie = cookies[i].trim();
                    if (cookie.substring(0, name.length + 1) === (name + '=')) {
                        cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                        break;
                    }
                }
            }
            return cookieValue;
        }
        </script>
        '''


class AlbumImageInlineForm(forms.ModelForm):
    """图集图片内联表单 - 支持直接上传文件"""
    class Meta:
        model = AlbumImage
        fields = ('image', 'order')
        widgets = {
            'order': forms.NumberInput(attrs={'min': '0', 'step': '1'}),
        }


class AlbumImageInline(admin.TabularInline):
    """图集图片内联 - 支持选择已有图片并排序"""
    model = AlbumImage
    form = AlbumImageInlineForm
    extra = 0
    max_num = 200
    raw_id_fields = ('image',)


@admin.register(Album)
class AlbumAdmin(admin.ModelAdmin):
    list_display = ('title', 'resource_id', 'author', 'image_count', 'created_at')
    search_fields = ('title', 'resource_id', 'author')
    readonly_fields = ('resource_id', 'created_at', 'updated_at', 'bulk_upload_button', 'sortable_images')
    filter_horizontal = ('categories',)
    inlines = [AlbumImageInline]

    def image_count(self, obj):
        """计算图集图片数量"""
        count = obj.album_images.count()
        if count > 0:
            return count
        return obj.images.count()
    image_count.short_description = '图片数量'

    def sortable_images(self, obj):
        """图集图片可拖拽排序区域 - 列表形式"""
        if obj is None or obj.id is None:
            return mark_safe('<p style="color: #999;">保存图集后此处可拖拽调整图片顺序</p>')

        # 查询当前图集所有图片，按 order 排序
        album_images = obj.album_images.select_related('image').order_by('order', 'id')
        if not album_images.exists():
            return mark_safe('<p style="color: #999;">暂无图片，请使用上方"批量上传图片"按钮上传</p>')

        items_html = ''
        for idx, ai in enumerate(album_images):
            img_url = ai.image.file.url
            img_id = ai.image.id
            filename = ai.image.file.name.split('/')[-1]
            items_html += f'''
            <div class="album-image-item" data-image-id="{img_id}"
                 style="display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: white; border: 1px solid #dee2e6; border-radius: 6px; margin-bottom: 6px; cursor: move; transition: box-shadow 0.15s, transform 0.15s;">
                <span class="album-drag-handle" style="color: #adb5bd; font-size: 18px; cursor: grab; user-select: none;">⋮⋮</span>
                <span class="album-order-num" style="display: inline-block; width: 36px; height: 36px; line-height: 36px; text-align: center; background: #28a745; color: white; border-radius: 50%; font-size: 13px; font-weight: bold; flex-shrink: 0;">{idx+1}</span>
                <img src="{img_url}" style="width: 48px; height: 64px; object-fit: cover; border-radius: 4px; flex-shrink: 0; pointer-events: none;">
                <span style="flex: 1; color: #495057; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; pointer-events: none;" title="{filename}">{filename}</span>
                <a href="{img_url}" target="_blank" style="color: #007bff; font-size: 12px; text-decoration: none; flex-shrink: 0;">查看原图</a>
            </div>
            '''

        album_id = obj.id
        return mark_safe(f'''
        <div style="margin-bottom: 8px; padding: 8px 12px; background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; color: #856404; font-size: 12px;">
            💡 拖拽整行可调整顺序（鼠标按住行任意位置拖动即可），调整后请点击"保存当前顺序"按钮
        </div>
        <div id="album_images_sortable" style="padding: 8px; background: #f8f9fa; border-radius: 6px; max-height: 600px; overflow-y: auto;">
            {items_html}
        </div>
        <div style="margin-top: 12px; text-align: center;">
            <button type="button" onclick="saveAlbumImageOrder({album_id})"
                    style="padding: 10px 24px; background: #28a745; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: bold;">
                💾 保存当前顺序
            </button>
            <span id="album_reorder_status" style="margin-left: 12px; font-size: 13px;"></span>
        </div>
        <script>
        (function() {{
            if (window.__albumSortableInit) return;
            window.__albumSortableInit = true;

            function initAlbumSortable() {{
                const container = document.getElementById('album_images_sortable');
                if (!container) return;
                if (typeof Sortable !== 'undefined') {{
                    createAlbumSortable();
                }} else {{
                    // 章节页未引入 Sortable 时补充加载
                    const s = document.createElement('script');
                    s.src = 'https://cdn.jsdelivr.net/npm/sortablejs@1.15.0/Sortable.min.js';
                    s.onload = createAlbumSortable;
                    document.head.appendChild(s);
                }}
            }}

            function createAlbumSortable() {{
                const container = document.getElementById('album_images_sortable');
                if (!container || !window.Sortable) return;
                Sortable.create(container, {{
                    animation: 200,
                    ghostClass: 'album-sortable-ghost',
                    chosenClass: 'album-sortable-chosen',
                    dragClass: 'album-sortable-drag',
                    onEnd: function() {{
                        const items = container.querySelectorAll('.album-image-item');
                        items.forEach(function(item, idx) {{
                            const numSpan = item.querySelector('.album-order-num');
                            if (numSpan) numSpan.textContent = idx + 1;
                        }});
                    }}
                }});
            }}

            if (document.readyState === 'loading') {{
                document.addEventListener('DOMContentLoaded', initAlbumSortable);
            }} else {{
                initAlbumSortable();
            }}
        }})();

        function saveAlbumImageOrder(albumId) {{
            const container = document.getElementById('album_images_sortable');
            if (!container) return;
            const items = container.querySelectorAll('.album-image-item');
            const newOrder = Array.from(items).map(function(el) {{
                return parseInt(el.getAttribute('data-image-id'));
            }});
            const statusSpan = document.getElementById('album_reorder_status');
            statusSpan.style.color = '#007bff';
            statusSpan.textContent = '⏳ 保存中...';
            const csrftoken = getCookie('csrftoken');
            fetch('/api/admin/albums/' + albumId + '/reorder-images/', {{
                method: 'POST',
                credentials: 'same-origin',
                headers: {{
                    'Content-Type': 'application/json',
                    'X-CSRFToken': csrftoken
                }},
                body: JSON.stringify({{ image_ids: newOrder }})
            }})
            .then(function(r) {{
                return r.json().then(function(data) {{ return {{ ok: r.ok, data: data }}; }});
            }})
            .then(function(result) {{
                if (result.ok && result.data.success) {{
                    statusSpan.style.color = '#28a745';
                    statusSpan.textContent = '✅ ' + (result.data.message || '保存成功');
                }} else {{
                    statusSpan.style.color = '#dc3545';
                    statusSpan.textContent = '❌ ' + (result.data.error || '保存失败');
                }}
            }})
            .catch(function(e) {{
                statusSpan.style.color = '#dc3545';
                statusSpan.textContent = '❌ 网络错误: ' + e.message;
            }});
        }}
        </script>
        <style>
        .album-sortable-ghost {{ opacity: 0.4; }}
        .album-sortable-chosen {{ box-shadow: 0 4px 12px rgba(40,167,69,0.4) !important; }}
        .album-sortable-drag {{ opacity: 0.95; box-shadow: 0 8px 24px rgba(0,0,0,0.2) !important; transform: rotate(1deg); }}
        .album-image-item:hover {{ box-shadow: 0 2px 8px rgba(0,0,0,0.12); }}
        .album-drag-handle:active {{ cursor: grabbing; }}
        </style>
        ''')
    sortable_images.short_description = '图片排序（可拖拽）'

    def formfield_for_foreignkey(self, db_field, request, **kwargs):
        """过滤封面图片下拉列表，只显示当前图集的图片"""
        if db_field.name == 'cover_image':
            # 获取当前编辑的图集对象
            try:
                object_id = request.resolver_match.kwargs.get('object_id')
                if object_id:
                    album = Album.objects.get(pk=object_id)
                    # 只显示属于当前图集的图片
                    kwargs['queryset'] = Image.objects.filter(
                        id__in=album.images.all()
                    )
                else:
                    # 新建图集时，还没有图片
                    kwargs['queryset'] = Image.objects.none()
            except (Album.DoesNotExist, Exception):
                kwargs['queryset'] = Image.objects.none()
        return super().formfield_for_foreignkey(db_field, request, **kwargs)

    def bulk_upload_button(self, obj):
        """批量上传图片按钮 - 顺序批次（保留原画质与选择顺序）"""
        if obj is None or obj.id is None:
            return mark_safe('''
            <div style="padding: 15px; background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; color: #856404;">
                ⚠️ 请先点击"保存图集"按钮创建图集，然后再上传图片
            </div>
            ''')

        album_id = obj.id
        return mark_safe(f'''
        <div style="padding: 20px; border: 2px dashed #007bff; border-radius: 8px; text-align: center; margin: 10px 0; background: #f8f9fa;">
            <p style="color: #333; margin-bottom: 10px; font-weight: bold;">📤 批量上传图片（保留原画质与选择顺序）</p>
            <p style="color: #666; margin-bottom: 15px; font-size: 12px;">
                按选择顺序分批上传（每批 5 张），保证最终顺序与选择顺序一致
            </p>
            <button type="button" onclick="document.getElementById('file_bulk_upload').click()" 
                    style="padding: 12px 24px; background: #007bff; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px;">
                📁 选择图片（多选）
            </button>
            <input type="file" id="file_bulk_upload" multiple accept="image/*" style="display: none;" 
                   onchange="startParallelUpload(this, {album_id})">
            <div id="upload_progress" style="margin-top: 15px; display: none;"></div>
            <div id="upload_result" style="margin-top: 10px;"></div>
        </div>
        <script>
        // ===== 并发池：限制同时执行的异步任务数 =====
        async function asyncPool(promises, limit) {{
            const ret = [];
            const executing = [];
            for (const p of promises) {{
                const e = Promise.resolve().then(p);
                ret.push(e);
                if (limit <= promises.length) {{
                    const ex = e.then(() => executing.splice(executing.indexOf(ex), 1));
                    executing.push(ex);
                    if (executing.length >= limit) {{
                        await Promise.race(executing);
                    }}
                }}
            }}
            return Promise.all(ret);
        }}

        // ===== 上传单个批次（5张） =====
        function uploadBatch(formData, csrftoken, albumId) {{
            return new Promise((resolve, reject) => {{
                const xhr = new XMLHttpRequest();
                xhr.open('POST', '/api/admin/albums/' + albumId + '/upload-images/');
                xhr.setRequestHeader('X-CSRFToken', csrftoken);
                xhr.onload = () => {{
                    if (xhr.status >= 200 && xhr.status < 300) {{
                        try {{ resolve(JSON.parse(xhr.responseText)); }} 
                        catch(e) {{ resolve({{success: true}}); }}
                    }} else {{
                        reject(new Error('HTTP ' + xhr.status));
                    }}
                }};
                xhr.onerror = () => reject(new Error('网络错误'));
                xhr.send(formData);
            }});
        }}

        // ===== 主上传函数：严格顺序批次（保留用户选择顺序） =====
        // 修复说明：原先并行 3 批会导致服务端按完成顺序追加，造成顺序乱。
        // 现在改为：按用户选择顺序分批，每批等待前一批完成再发下一批，保证顺序。
        async function startParallelUpload(input, albumId) {{
            const files = Array.from(input.files);
            if (files.length === 0) return;

            const progressDiv = document.getElementById('upload_progress');
            const resultDiv = document.getElementById('upload_result');
            progressDiv.style.display = 'block';
            resultDiv.innerHTML = '';

            const BATCH_SIZE = 5;      // 每批 5 张
            const csrftoken = getCookie('csrftoken');

            // 按选择顺序分批
            const batches = [];
            for (let i = 0; i < files.length; i += BATCH_SIZE) {{
                batches.push(files.slice(i, i + BATCH_SIZE));
            }}

            const totalBatches = batches.length;
            let completedBatches = 0;
            let totalUploaded = 0;
            let totalFailed = 0;

            progressDiv.innerHTML =
                '<div style="text-align:center;color:#28a745;font-size:13px;margin-bottom:8px;">📤 顺序上传 ' + totalBatches + ' 批 / 共 ' + files.length + ' 张</div>' +
                '<div style="background:#e9ecef;border-radius:4px;height:10px;overflow:hidden;"><div id="upload_bar" style="background:#28a745;height:100%;width:0%;transition:width 0.3s;"></div></div>' +
                '<p id="upload_text" style="margin:8px 0 0;font-size:12px;color:#666;text-align:center;">准备上传...</p>';

            // 严格顺序：逐批上传，等待前一批完成才发下一批
            for (let batchIdx = 0; batchIdx < batches.length; batchIdx++) {{
                const batch = batches[batchIdx];
                const formData = new FormData();
                batch.forEach((file) => {{
                    formData.append('images', file, file.name);
                }});
                formData.append('album_id', albumId);

                try {{
                    const result = await uploadBatch(formData, csrftoken, albumId);
                    totalUploaded += result.uploaded_count || batch.length;
                }} catch (e) {{
                    totalFailed += batch.length;
                    console.error('Batch ' + batchIdx + ' failed:', e.message);
                }}

                completedBatches++;
                const pct = Math.round(completedBatches / totalBatches * 100);
                document.getElementById('upload_bar').style.width = pct + '%';
                document.getElementById('upload_text').textContent =
                    '上传中 ' + completedBatches + '/' + totalBatches + ' 批 | ✅' + totalUploaded + ' ❌' + totalFailed;
            }}

            progressDiv.style.display = 'none';
            const hasFail = totalFailed > 0;
            resultDiv.innerHTML =
                '<div style="padding:15px;background:' + (hasFail ? '#fff3cd;color:#856404;' : '#d4edda;color:#155724;') +
                'border-radius:6px;font-size:14px;">' +
                '🎉 上传完成！<br>' +
                '✅ 成功: ' + totalUploaded + ' 张<br>' +
                (hasFail ? '❌ 失败: ' + totalFailed + ' 张' : '') +
                '</div>';
            setTimeout(() => {{ location.reload(); }}, 2000);
        }}

        function getCookie(name) {{
            var cookieValue = null;
            if (document.cookie && document.cookie !== '') {{
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {{
                    var cookie = cookies[i].trim();
                    if (cookie.substring(0, name.length + 1) === (name + '=')) {{
                        cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                        break;
                    }}
                }}
            }}
            return cookieValue;
        }}
        </script>
        ''')
    bulk_upload_button.short_description = '批量上传图片'

    fieldsets = (
        ('基本信息', {
            'fields': ('title', 'description', 'author'),
            'description': '设置图集的标题、描述和作者'
        }),
        ('封面图片', {
            'fields': ('cover_image',),
            'description': '选择图集的封面图片（从已上传的图片中选择）'
        }),
        ('标签关联', {
            'fields': ('categories',),
            'description': '为图集选择一个或多个标签'
        }),
        ('批量上传图片', {
            'fields': ('bulk_upload_button',),
            'description': '保存图集后可通过此按钮批量上传图片（支持一次选择几十张）'
        }),
        ('图片排序（可拖拽）', {
            'fields': ('sortable_images',),
            'description': '保存图集后可拖拽图片调整顺序，调整后点击"保存当前顺序"按钮'
        }),
        ('系统信息', {
            'classes': ('collapse',),
            'fields': ('resource_id', 'created_at', 'updated_at')
        })
    )

    def save_model(self, request, obj, form, change):
        """保存模型"""
        super().save_model(request, obj, form, change)

    def save_related(self, request, form, formsets, change):
        """保存关联的内联表单"""
        super().save_related(request, form, formsets, change)


class ComicChapterInline(admin.TabularInline):
    """漫画章节内联 - 在漫画编辑页显示章节列表（仅显示标题和章节号，图片在独立章节页上传）"""
    model = ComicChapter
    extra = 1
    fields = ('title', 'chapter_number')


@admin.register(Comic)
class ComicAdmin(admin.ModelAdmin):
    list_display = ('title', 'resource_id', 'slug', 'author', 'chapter_count', 'created_at')
    search_fields = ('title', 'resource_id', 'slug')
    list_filter = ('categories',)
    readonly_fields = ('resource_id', 'created_at', 'updated_at', 'bulk_cover_upload_button', 'cover_preview')
    prepopulated_fields = {'slug': ('title',)}
    filter_horizontal = ('categories',)
    inlines = [ComicChapterInline]

    def chapter_count(self, obj):
        return obj.chapters.count()
    chapter_count.short_description = '章节数'

    def cover_preview(self, obj):
        """显示当前封面图片预览"""
        if not obj or not obj.cover_image:
            return mark_safe('<p style="color: #999;">暂无封面，请使用下方按钮上传</p>')
        cover_url = obj.cover_image.url
        return mark_safe(f'''
        <div style="margin-top: 8px;">
            <img src="{cover_url}" style="max-width: 200px; max-height: 280px; border: 1px solid #ddd; border-radius: 4px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
        </div>
        ''')
    cover_preview.short_description = '当前封面预览'

    def bulk_cover_upload_button(self, obj):
        """漫画封面上传按钮 - 单文件上传"""
        if obj is None or obj.id is None:
            return mark_safe('''
            <div style="padding: 15px; background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; color: #856404;">
                ⚠️ 请先点击"保存漫画"按钮创建漫画，然后再上传封面
            </div>
            ''')

        comic_id = obj.id
        return mark_safe(f'''
        <div style="padding: 20px; border: 2px dashed #007bff; border-radius: 8px; text-align: center; margin: 10px 0; background: #f8f9fa;">
            <p style="color: #333; margin-bottom: 10px; font-weight: bold;">📤 上传漫画封面</p>
            <p style="color: #666; margin-bottom: 15px; font-size: 12px;">
                支持 JPG / PNG / WebP / GIF / BMP，建议比例 2:3
            </p>
            <button type="button" onclick="document.getElementById('file_cover_upload').click()"
                    style="padding: 12px 24px; background: #007bff; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px;">
                📁 选择封面图片
            </button>
            <input type="file" id="file_cover_upload" accept="image/*" style="display: none;"
                   onchange="uploadCover(this, {comic_id})">
            <div id="cover_upload_progress" style="margin-top: 15px; display: none;"></div>
        </div>
        <script>
        function uploadCover(input, comicId) {{
            const file = input.files[0];
            if (!file) return;

            const progressDiv = document.getElementById('cover_upload_progress');
            progressDiv.style.display = 'block';
            progressDiv.innerHTML = '<div style="color: #007bff; font-size: 13px;">⏳ 上传中...</div>';

            const formData = new FormData();
            formData.append('cover', file);

            const csrftoken = getCookie('csrftoken');
            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/admin/comics/' + comicId + '/upload-cover/');
            xhr.setRequestHeader('X-CSRFToken', csrftoken);
            xhr.onload = () => {{
                if (xhr.status >= 200 && xhr.status < 300) {{
                    try {{
                        const result = JSON.parse(xhr.responseText);
                        progressDiv.innerHTML = '<div style="color: #28a745; font-size: 13px;">✅ ' + (result.message || '上传成功') + '</div>';
                    }} catch (e) {{
                        progressDiv.innerHTML = '<div style="color: #28a745; font-size: 13px;">✅ 上传成功</div>';
                    }}
                    setTimeout(() => {{ location.reload(); }}, 1500);
                }} else {{
                    let errMsg = '上传失败: HTTP ' + xhr.status;
                    try {{
                        const err = JSON.parse(xhr.responseText);
                        if (err.error) errMsg = '❌ ' + err.error;
                    }} catch (e) {{}}
                    progressDiv.innerHTML = '<div style="color: #dc3545; font-size: 13px;">' + errMsg + '</div>';
                }}
            }};
            xhr.onerror = () => {{
                progressDiv.innerHTML = '<div style="color: #dc3545; font-size: 13px;">❌ 网络错误</div>';
            }};
            xhr.send(formData);
        }}

        function getCookie(name) {{
            var cookieValue = null;
            if (document.cookie && document.cookie !== '') {{
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {{
                    var cookie = cookies[i].trim();
                    if (cookie.substring(0, name.length + 1) === (name + '=')) {{
                        cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                        break;
                    }}
                }}
            }}
            return cookieValue;
        }}
        </script>
        ''')
    bulk_cover_upload_button.short_description = '快速上传封面'

    fieldsets = (
        ('基本信息', {
            'fields': ('title', 'slug', 'description', 'author'),
            'description': '设置漫画的标题、描述和作者'
        }),
        ('封面图片', {
            'fields': ('cover_image', 'cover_preview', 'bulk_cover_upload_button'),
            'description': '可直接选择文件上传封面，或使用下方"快速上传封面"按钮（推荐）'
        }),
        ('标签关联', {
            'fields': ('categories',),
            'description': '为漫画选择一个或多个标签'
        }),
        ('系统信息', {
            'classes': ('collapse',),
            'fields': ('resource_id', 'created_at', 'updated_at')
        })
    )


@admin.register(ComicChapter)
class ComicChapterAdmin(admin.ModelAdmin):
    """漫画章节管理 - 支持批量上传章节图片"""
    list_display = ('title', 'comic', 'chapter_number', 'image_count', 'created_at')
    search_fields = ('title', 'comic__title')
    list_filter = ('comic',)
    readonly_fields = ('bulk_upload_images_button', 'image_preview', 'created_at')

    def image_count(self, obj):
        return len(obj.images) if obj.images else 0
    image_count.short_description = '图片数'

    def image_preview(self, obj):
        """显示当前章节所有图片预览 - 列表形式支持拖拽排序"""
        if not obj.images:
            return mark_safe('<p style="color: #999;">暂无图片，请使用下方按钮上传</p>')

        # 构建可拖拽图片列表（单列列表形式）
        items_html = ''
        for idx, img_path in enumerate(obj.images):
            img_url = f'/media/{img_path}'
            # 提取文件名作为显示文本
            filename = img_path.split('/')[-1]
            items_html += f'''
            <div class="chapter-image-item" data-path="{img_path}"
                 style="display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: white; border: 1px solid #dee2e6; border-radius: 6px; margin-bottom: 6px; cursor: move; transition: box-shadow 0.15s, transform 0.15s;">
                <span class="drag-handle" style="color: #adb5bd; font-size: 18px; cursor: grab; user-select: none;">⋮⋮</span>
                <span class="image-order-num" style="display: inline-block; width: 36px; height: 36px; line-height: 36px; text-align: center; background: #007bff; color: white; border-radius: 50%; font-size: 13px; font-weight: bold; flex-shrink: 0;">{idx+1}</span>
                <img src="{img_url}" style="width: 48px; height: 64px; object-fit: cover; border-radius: 4px; flex-shrink: 0; pointer-events: none;">
                <span style="flex: 1; color: #495057; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; pointer-events: none;" title="{filename}">{filename}</span>
                <a href="{img_url}" target="_blank" style="color: #007bff; font-size: 12px; text-decoration: none; flex-shrink: 0;">查看原图</a>
            </div>
            '''

        chapter_id = obj.id
        return mark_safe(f'''
        <div style="margin-bottom: 8px; padding: 8px 12px; background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; color: #856404; font-size: 12px;">
            💡 拖拽整行可调整顺序（鼠标按住行任意位置拖动即可），调整后请点击"保存当前顺序"按钮
        </div>
        <div id="chapter_images_sortable" style="padding: 8px; background: #f8f9fa; border-radius: 6px; max-height: 600px; overflow-y: auto;">
            {items_html}
        </div>
        <div style="margin-top: 12px; text-align: center;">
            <button type="button" onclick="saveChapterImageOrder({chapter_id})"
                    style="padding: 10px 24px; background: #28a745; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: bold;">
                💾 保存当前顺序
            </button>
            <span id="reorder_status" style="margin-left: 12px; font-size: 13px;"></span>
        </div>
        <script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.0/Sortable.min.js"></script>
        <script>
        (function() {{
            // 防止重复初始化
            if (window.__chapterSortableInit) return;
            window.__chapterSortableInit = true;

            function initSortable() {{
                const container = document.getElementById('chapter_images_sortable');
                if (container && typeof Sortable !== 'undefined') {{
                    Sortable.create(container, {{
                        animation: 200,
                        ghostClass: 'sortable-ghost',
                        chosenClass: 'sortable-chosen',
                        dragClass: 'sortable-drag',
                        onEnd: function() {{
                            // 实时更新显示的序号
                            const items = container.querySelectorAll('.chapter-image-item');
                            items.forEach(function(item, idx) {{
                                const numSpan = item.querySelector('.image-order-num');
                                if (numSpan) numSpan.textContent = idx + 1;
                            }});
                        }}
                    }});
                }}
            }}

            if (document.readyState === 'loading') {{
                document.addEventListener('DOMContentLoaded', initSortable);
            }} else {{
                initSortable();
            }}
        }})();

        function saveChapterImageOrder(chapterId) {{
            const container = document.getElementById('chapter_images_sortable');
            if (!container) return;

            const items = container.querySelectorAll('.chapter-image-item');
            const newOrder = Array.from(items).map(function(el) {{
                return el.getAttribute('data-path');
            }});

            const statusSpan = document.getElementById('reorder_status');
            statusSpan.style.color = '#007bff';
            statusSpan.textContent = '⏳ 保存中...';

            const csrftoken = getCookie('csrftoken');
            fetch('/api/admin/comic-chapters/' + chapterId + '/reorder-images/', {{
                method: 'POST',
                credentials: 'same-origin',
                headers: {{
                    'Content-Type': 'application/json',
                    'X-CSRFToken': csrftoken
                }},
                body: JSON.stringify({{ images: newOrder }})
            }})
            .then(function(r) {{
                return r.json().then(function(data) {{ return {{ ok: r.ok, data: data }}; }});
            }})
            .then(function(result) {{
                if (result.ok && result.data.success) {{
                    statusSpan.style.color = '#28a745';
                    statusSpan.textContent = '✅ ' + (result.data.message || '保存成功');
                }} else {{
                    statusSpan.style.color = '#dc3545';
                    statusSpan.textContent = '❌ ' + (result.data.error || '保存失败');
                }}
            }})
            .catch(function(e) {{
                statusSpan.style.color = '#dc3545';
                statusSpan.textContent = '❌ 网络错误: ' + e.message;
            }});
        }}

        function getCookie(name) {{
            var cookieValue = null;
            if (document.cookie && document.cookie !== '') {{
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {{
                    var cookie = cookies[i].trim();
                    if (cookie.substring(0, name.length + 1) === (name + '=')) {{
                        cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                        break;
                    }}
                }}
            }}
            return cookieValue;
        }}
        </script>
        <style>
        .sortable-ghost {{ opacity: 0.4; }}
        .sortable-chosen {{ box-shadow: 0 4px 12px rgba(0,123,255,0.4) !important; }}
        .sortable-drag {{ opacity: 0.95; box-shadow: 0 8px 24px rgba(0,0,0,0.2) !important; transform: rotate(1deg); }}
        .chapter-image-item:hover {{ box-shadow: 0 2px 8px rgba(0,0,0,0.12); }}
        .drag-handle:active {{ cursor: grabbing; }}
        </style>
        ''')
    image_preview.short_description = '当前图片预览（可拖拽排序）'

    def bulk_upload_images_button(self, obj):
        """批量上传章节图片按钮 - 顺序批次（保留原画质与选择顺序）"""
        if obj is None or obj.id is None:
            return mark_safe('''
            <div style="padding: 15px; background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; color: #856404;">
                ⚠️ 请先点击"保存章节"按钮创建章节，然后再上传图片
            </div>
            ''')

        chapter_id = obj.id
        return mark_safe(f'''
        <div style="padding: 20px; border: 2px dashed #007bff; border-radius: 8px; text-align: center; margin: 10px 0; background: #f8f9fa;">
            <p style="color: #333; margin-bottom: 10px; font-weight: bold;">📤 批量上传章节图片（保留原画质与选择顺序）</p>
            <p style="color: #666; margin-bottom: 15px; font-size: 12px;">
                按选择顺序分批上传（每批 5 张），保证最终顺序与选择顺序一致
            </p>
            <button type="button" onclick="document.getElementById('file_chapter_bulk_upload').click()"
                    style="padding: 12px 24px; background: #007bff; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 14px;">
                📁 选择图片（多选）
            </button>
            <input type="file" id="file_chapter_bulk_upload" multiple accept="image/*" style="display: none;"
                   onchange="startParallelUpload(this, {chapter_id})">
            <div id="upload_progress" style="margin-top: 15px; display: none;"></div>
            <div id="upload_result" style="margin-top: 10px;"></div>
        </div>
        <script>
        // ===== 并发池：限制同时执行的异步任务数 =====
        async function asyncPool(promises, limit) {{
            const ret = [];
            const executing = [];
            for (const p of promises) {{
                const e = Promise.resolve().then(p);
                ret.push(e);
                if (limit <= promises.length) {{
                    const ex = e.then(() => executing.splice(executing.indexOf(ex), 1));
                    executing.push(ex);
                    if (executing.length >= limit) {{
                        await Promise.race(executing);
                    }}
                }}
            }}
            return Promise.all(ret);
        }}

        // ===== 上传单个批次（5张） =====
        function uploadBatch(formData, csrftoken, chapterId) {{
            return new Promise((resolve, reject) => {{
                const xhr = new XMLHttpRequest();
                xhr.open('POST', '/api/admin/comic-chapters/' + chapterId + '/upload-images/');
                xhr.setRequestHeader('X-CSRFToken', csrftoken);
                xhr.onload = () => {{
                    if (xhr.status >= 200 && xhr.status < 300) {{
                        try {{ resolve(JSON.parse(xhr.responseText)); }}
                        catch(e) {{ resolve({{success: true}}); }}
                    }} else {{
                        let errMsg = 'HTTP ' + xhr.status;
                        try {{
                            const err = JSON.parse(xhr.responseText);
                            if (err.error) errMsg += ': ' + err.error;
                        }} catch(e) {{}}
                        reject(new Error(errMsg));
                    }}
                }};
                xhr.onerror = () => reject(new Error('网络错误'));
                xhr.send(formData);
            }});
        }}

        // ===== 主上传函数：严格顺序批次（保留用户选择顺序） =====
        // 修复说明：原先并行 3 批会导致服务端按完成顺序追加，造成顺序乱。
        // 现在改为：按用户选择顺序分批，每批等待前一批完成再发下一批，保证顺序。
        async function startParallelUpload(input, chapterId) {{
            const files = Array.from(input.files);
            if (files.length === 0) return;

            const progressDiv = document.getElementById('upload_progress');
            const resultDiv = document.getElementById('upload_result');
            progressDiv.style.display = 'block';
            resultDiv.innerHTML = '';

            const BATCH_SIZE = 5;      // 每批 5 张
            const csrftoken = getCookie('csrftoken');

            // 按选择顺序分批
            const batches = [];
            for (let i = 0; i < files.length; i += BATCH_SIZE) {{
                batches.push(files.slice(i, i + BATCH_SIZE));
            }}

            const totalBatches = batches.length;
            let completedBatches = 0;
            let totalUploaded = 0;
            let totalFailed = 0;

            progressDiv.innerHTML =
                '<div style="text-align:center;color:#28a745;font-size:13px;margin-bottom:8px;">📤 顺序上传 ' + totalBatches + ' 批 / 共 ' + files.length + ' 张</div>' +
                '<div style="background:#e9ecef;border-radius:4px;height:10px;overflow:hidden;"><div id="upload_bar" style="background:#28a745;height:100%;width:0%;transition:width 0.3s;"></div></div>' +
                '<p id="upload_text" style="margin:8px 0 0;font-size:12px;color:#666;text-align:center;">准备上传...</p>';

            // 严格顺序：逐批上传，等待前一批完成才发下一批
            for (let batchIdx = 0; batchIdx < batches.length; batchIdx++) {{
                const batch = batches[batchIdx];
                const formData = new FormData();
                batch.forEach((file) => {{
                    formData.append('images', file, file.name);
                }});

                try {{
                    const result = await uploadBatch(formData, csrftoken, chapterId);
                    totalUploaded += result.uploaded_count || batch.length;
                }} catch (e) {{
                    totalFailed += batch.length;
                    console.error('Batch ' + batchIdx + ' failed:', e.message);
                }}

                completedBatches++;
                const pct = Math.round(completedBatches / totalBatches * 100);
                document.getElementById('upload_bar').style.width = pct + '%';
                document.getElementById('upload_text').textContent =
                    '上传中 ' + completedBatches + '/' + totalBatches + ' 批 | ✅' + totalUploaded + ' ❌' + totalFailed;
            }}

            progressDiv.style.display = 'none';
            const hasFail = totalFailed > 0;
            resultDiv.innerHTML =
                '<div style="padding:15px;background:' + (hasFail ? '#fff3cd;color:#856404;' : '#d4edda;color:#155724;') +
                'border-radius:6px;font-size:14px;">' +
                '🎉 上传完成！<br>' +
                '✅ 成功: ' + totalUploaded + ' 张<br>' +
                (hasFail ? '❌ 失败: ' + totalFailed + ' 张' : '') +
                '</div>';
            setTimeout(() => {{ location.reload(); }}, 2000);
        }}

        function getCookie(name) {{
            var cookieValue = null;
            if (document.cookie && document.cookie !== '') {{
                var cookies = document.cookie.split(';');
                for (var i = 0; i < cookies.length; i++) {{
                    var cookie = cookies[i].trim();
                    if (cookie.substring(0, name.length + 1) === (name + '=')) {{
                        cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                        break;
                    }}
                }}
            }}
            return cookieValue;
        }}
        </script>
        ''')
    bulk_upload_images_button.short_description = '批量上传图片'

    fieldsets = (
        ('章节信息', {
            'fields': ('comic', 'title', 'chapter_number'),
            'description': '设置章节所属的漫画、标题和章节号'
        }),
        ('批量上传图片', {
            'fields': ('bulk_upload_images_button',),
            'description': '保存章节后可通过此按钮批量上传图片（支持一次选择几十张）'
        }),
        ('当前图片预览', {
            'classes': ('collapse',),
            'fields': ('image_preview',),
            'description': '当前章节已上传的所有图片'
        }),
        ('系统信息', {
            'classes': ('collapse',),
            'fields': ('created_at',)
        })
    )


@admin.register(Video)
class VideoAdmin(admin.ModelAdmin):
    list_display = ('title', 'resource_id', 'slug', 'video_type', 'duration_display', 'created_at')
    list_filter = ('video_type', 'categories')
    search_fields = ('title', 'resource_id', 'slug')
    readonly_fields = ('resource_id', 'created_at', 'updated_at')
    prepopulated_fields = {'slug': ('title',)}
    filter_horizontal = ('categories',)

    def duration_display(self, obj):
        if obj.duration:
            minutes, seconds = divmod(obj.duration, 60)
            hours, minutes = divmod(minutes, 60)
            if hours > 0:
                return f'{hours}:{minutes:02d}:{seconds:02d}'
            return f'{minutes}:{seconds:02d}'
        return '-'
    duration_display.short_description = '时长'

    fieldsets = (
        ('基本信息', {
            'fields': ('title', 'slug', 'description'),
            'description': '设置视频的标题、标识和描述'
        }),
        ('文件上传', {
            'fields': ('video_file', 'thumbnail', 'm3u8_path'),
            'description': '上传视频文件或设置M3U8路径，可选择上传缩略图'
        }),
        ('视频属性', {
            'fields': ('video_type', 'duration', 'tags'),
            'description': '设置视频类型（视频/特辑）、时长和附加标签'
        }),
        ('标签关联', {
            'fields': ('categories',),
            'description': '为视频选择一个或多个标签'
        }),
        ('系统信息', {
            'classes': ('collapse',),
            'fields': ('resource_id', 'created_at', 'updated_at')
        })
    )


@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'user_type')
    list_filter = ('user_type',)
    search_fields = ('user__username',)


@admin.register(Note)
class NoteAdmin(admin.ModelAdmin):
    list_display = ('title', 'user', 'created_at')
    search_fields = ('title', 'user__username')
    list_filter = ('user',)


@admin.register(CloudFile)
class CloudFileAdmin(admin.ModelAdmin):
    list_display = ('name', 'file_type', 'file_size', 'user', 'created_at')
    list_filter = ('file_type', 'user')
    search_fields = ('name',)
    readonly_fields = ('created_at', 'updated_at')


admin.site.register(Category, CategoryAdmin)
