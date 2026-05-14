<template>
  <div class="ds-field ds-rich-text">
    <label v-if="label" class="ds-field__label">
      <span v-if="required" class="ds-field__required">*</span>
      <span>{{ label }}</span>
    </label>
    <div class="ds-rich-text__shell" :class="{ 'is-disabled': disabled }">
      <div v-if="editor" class="ds-rich-text__toolbar">
        <button type="button" :class="{ active: editor.isActive('bold') }" @click="editor.chain().focus().toggleBold().run()">B</button>
        <button type="button" :class="{ active: editor.isActive('italic') }" @click="editor.chain().focus().toggleItalic().run()">I</button>
        <button type="button" :class="{ active: editor.isActive('underline') }" @click="editor.chain().focus().toggleUnderline().run()">U</button>
        <button type="button" :class="{ active: editor.isActive('heading', { level: 2 }) }" @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">标题</button>
        <button type="button" :class="{ active: editor.isActive('bulletList') }" @click="editor.chain().focus().toggleBulletList().run()">列表</button>
        <button type="button" :class="{ active: editor.isActive('orderedList') }" @click="editor.chain().focus().toggleOrderedList().run()">序号</button>
        <select :value="currentFontSize" @change="setFontSize($event.target.value)">
          <option value="">字号</option>
          <option value="12px">12</option>
          <option value="14px">14</option>
          <option value="16px">16</option>
          <option value="18px">18</option>
          <option value="24px">24</option>
        </select>
        <input class="ds-rich-text__color" type="color" :value="currentColor" @input="setColor($event.target.value)" />
        <button type="button" @click="setLink">链接</button>
        <button type="button" @click="editor.chain().focus().unsetAllMarks().clearNodes().run()">清除</button>
      </div>
      <EditorContent :editor="editor" class="ds-rich-text__content" />
    </div>
    <div v-if="uploading" class="ds-field__hint">图片上传中...</div>
    <div v-else-if="hint" class="ds-field__hint">{{ hint }}</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import Link from '@tiptap/extension-link'
import { TextStyle } from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Image from '@tiptap/extension-image'
import { Extension } from '@tiptap/core'

const FontSize = Extension.create({
  name: 'fontSize',
  addGlobalAttributes() {
    return [
      {
        types: ['textStyle'],
        attributes: {
          fontSize: {
            default: null,
            parseHTML: element => element.style.fontSize || null,
            renderHTML: attributes => {
              if (!attributes.fontSize) return {}
              return { style: `font-size: ${attributes.fontSize}` }
            }
          }
        }
      }
    ]
  },
  addCommands() {
    return {
      setFontSize: fontSize => ({ chain }) => chain().setMark('textStyle', { fontSize }).run(),
      unsetFontSize: () => ({ chain }) => chain().setMark('textStyle', { fontSize: null }).removeEmptyTextStyle().run()
    }
  }
})

const props = defineProps({
  modelValue: { type: String, default: '' },
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  required: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  uploadImage: { type: Function, default: null }
})

const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

const editor = useEditor({
  content: props.modelValue || '',
  editable: !props.disabled,
  extensions: [
    StarterKit,
    Underline,
    TextStyle,
    Color,
    FontSize,
    Image.configure({ inline: false, allowBase64: false }),
    Link.configure({ openOnClick: false, autolink: true })
  ],
  editorProps: {
    handlePaste(view, event) {
      const files = Array.from(event.clipboardData?.files || []).filter(file => file.type.startsWith('image/'))
      if (!files.length || !props.uploadImage) return false
      event.preventDefault()
      uploadImages(files)
      return true
    }
  },
  onUpdate({ editor: currentEditor }) {
    emit('update:modelValue', currentEditor.getHTML())
  }
})

const currentColor = computed(() => editor.value?.getAttributes('textStyle')?.color || 'var(--ds-color-primary)')
const currentFontSize = computed(() => editor.value?.getAttributes('textStyle')?.fontSize || '')

const setColor = color => {
  editor.value?.chain().focus().setColor(color).run()
}

const setFontSize = size => {
  if (size) editor.value?.chain().focus().setFontSize(size).run()
  else editor.value?.chain().focus().unsetFontSize().run()
}

const setLink = () => {
  const previousUrl = editor.value?.getAttributes('link')?.href || ''
  const url = window.prompt('请输入链接地址', previousUrl)
  if (url === null) return
  if (!url) {
    editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  editor.value?.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
}

const uploadImages = async (files) => {
  uploading.value = true
  try {
    for (const file of files) {
      const url = await props.uploadImage(file)
      if (url) editor.value?.chain().focus().setImage({ src: url }).run()
    }
  } finally {
    uploading.value = false
  }
}

watch(() => props.modelValue, value => {
  if (!editor.value) return
  if (value !== editor.value.getHTML()) {
    editor.value.commands.setContent(value || '', false)
  }
})

watch(() => props.disabled, value => {
  editor.value?.setEditable(!value)
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped>
.ds-rich-text__shell {
  border: 1px solid var(--ds-color-border);
  border-radius: 10px;
  background: var(--ds-color-bg-card);
  overflow: hidden;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.ds-rich-text__shell:focus-within {
  border-color: var(--ds-color-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ds-color-primary) 10%, transparent);
}

.ds-rich-text__shell.is-disabled {
  background: var(--ds-color-bg-soft);
}

.ds-rich-text__toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;
  border-bottom: 1px solid var(--ds-color-border);
  background: var(--ds-color-bg-soft);
}

.ds-rich-text__toolbar button,
.ds-rich-text__toolbar select {
  height: 28px;
  border: 1px solid var(--ds-color-border);
  border-radius: 6px;
  background: var(--ds-color-bg-card);
  color: var(--ds-color-text-regular);
  cursor: pointer;
  font-size: 12px;
}

.ds-rich-text__toolbar button {
  min-width: 30px;
  padding: 0 8px;
}

.ds-rich-text__toolbar button.active,
.ds-rich-text__toolbar button:hover {
  border-color: var(--ds-color-primary);
  background: var(--ds-color-bg-selected);
  color: var(--ds-color-primary);
}

.ds-rich-text__color {
  width: 30px;
  height: 28px;
  padding: 2px;
  border: 1px solid var(--ds-color-border);
  border-radius: 6px;
  background: var(--ds-color-bg-card);
}

.ds-rich-text__content :deep(.ProseMirror) {
  min-height: 180px;
  padding: 14px 16px;
  color: var(--ds-color-text-primary);
  line-height: 1.8;
  outline: none;
}

.ds-rich-text__content :deep(.ProseMirror p) {
  margin: 0 0 8px;
}

.ds-rich-text__content :deep(.ProseMirror img) {
  max-width: 100%;
  border-radius: 8px;
}
</style>
