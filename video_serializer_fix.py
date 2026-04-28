class VideoCreateUpdateSerializer(serializers.ModelSerializer):
    categories = serializers.PrimaryKeyRelatedField(queryset=Category.objects.all(), many=True)
    thumbnail = serializers.CharField(required=False, allow_blank=True, max_length=100)
    video_file = serializers.CharField(required=False, allow_blank=True, max_length=100)

    class Meta:
        model = Video
        fields = ['title', 'slug', 'description', 'thumbnail', 'video_file', 'categories', 'duration']

    def _convert_path_to_file(self, file_path):
        import os
        from django.core.files.base import ContentFile

        if not file_path:
            return None

        if file_path.startswith('/media/'):
            file_path = file_path[7:]
        elif file_path.startswith('media/'):
            file_path = file_path[6:]

        full_path = os.path.join(settings.MEDIA_ROOT, file_path)
        if os.path.exists(full_path):
            with open(full_path, 'rb') as f:
                file_content = ContentFile(f.read())
                file_content.name = os.path.basename(file_path)
                return file_content
        return None

    def create(self, validated_data):
        categories = validated_data.pop('categories', [])

        thumbnail_path = validated_data.pop('thumbnail', None)
        video_file_path = validated_data.pop('video_file', None)

        if thumbnail_path:
            validated_data['thumbnail'] = self._convert_path_to_file(thumbnail_path)
        if video_file_path:
            validated_data['video_file'] = self._convert_path_to_file(video_file_path)

        video = super().create(validated_data)
        if categories:
            video.categories.set(categories)
        return video

    def update(self, instance, validated_data):
        categories = validated_data.pop('categories', [])

        thumbnail_path = validated_data.pop('thumbnail', None)
        video_file_path = validated_data.pop('video_file', None)

        if thumbnail_path:
            validated_data['thumbnail'] = self._convert_path_to_file(thumbnail_path)
        if video_file_path:
            validated_data['video_file'] = self._convert_path_to_file(video_file_path)

        instance = super().update(instance, validated_data)
        if categories:
            instance.categories.set(categories)
        return instance
