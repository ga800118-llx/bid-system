import './tokens/colors.css'
import './tokens/radius.css'
import './tokens/shadow.css'
import './tokens/spacing.css'
import './tokens/form.css'

import DsPageHeader from './layout/DsPageHeader.vue'
import DsHeaderActions from './layout/DsHeaderActions.vue'
import DsTopBar from './layout/DsTopBar.vue'

import DsInput from './form/DsInput.vue'
import DsPasswordInput from './form/DsPasswordInput.vue'
import DsNumberInput from './form/DsNumberInput.vue'
import DsAmountInput from './form/DsAmountInput.vue'
import DsTextarea from './form/DsTextarea.vue'
import DsRichTextEditor from './form/DsRichTextEditor.vue'
import DsSwitch from './form/DsSwitch.vue'
import DsCheckboxGroup from './form/DsCheckboxGroup.vue'
import DsRadioGroup from './form/DsRadioGroup.vue'
import DsDatePicker from './form/DsDatePicker.vue'
import DsDateRangePicker from './form/DsDateRangePicker.vue'
import DsModalForm from './form/DsModalForm.vue'
import DsFormSection from './form/DsFormSection.vue'
import DsFormGrid from './form/DsFormGrid.vue'
import DsFormActions from './form/DsFormActions.vue'
import DsReadonlyField from './form/DsReadonlyField.vue'

import DsDeptTreeSelect from './selectors/DsDeptTreeSelect.vue'
import DsUserAvatarSelect from './selectors/DsUserAvatarSelect.vue'
import DsRoleMultiSelect from './selectors/DsRoleMultiSelect.vue'
import DsDictSelect from './selectors/DsDictSelect.vue'
import DsStatusSelect from './selectors/DsStatusSelect.vue'
import DsCascaderSelect from './selectors/DsCascaderSelect.vue'
import DsFileCategorySelect from './selectors/DsFileCategorySelect.vue'

import DsFilterBar from './filter/DsFilterBar.vue'
import DsAdvancedFilterPanel from './filter/DsAdvancedFilterPanel.vue'
import DsKeywordSearch from './filter/DsKeywordSearch.vue'

import DsStatusTag from './display/DsStatusTag.vue'
import DsRoleTag from './display/DsRoleTag.vue'
import DsTag from './display/DsTag.vue'
import DsUserAvatar from './display/DsUserAvatar.vue'
import DsPriorityStars from './display/DsPriorityStars.vue'
import DsEmptyState from './display/DsEmptyState.vue'
import DsSecurityPopover from './display/DsSecurityPopover.vue'
import DsSecurityStatus from './display/DsSecurityStatus.vue'
import DsStatsCard from './display/DsStatsCard.vue'
import DsSecuritySummaryCard from './display/DsSecuritySummaryCard.vue'
import DsActionBar from './display/DsActionBar.vue'
import DsBatchActions from './display/DsBatchActions.vue'
import DsIconButton from './display/DsIconButton.vue'
import DsDataTable from './display/DsDataTable.vue'
import DsUserCell from './display/DsUserCell.vue'
import DsPagination from './display/DsPagination.vue'
import { DS_INPUT_TYPES, normalizeDsInputValue, validateDsInputValue } from './form/validators'

const components = [
  DsInput,
  DsPasswordInput,
  DsNumberInput,
  DsAmountInput,
  DsTextarea,
  DsRichTextEditor,
  DsSwitch,
  DsCheckboxGroup,
  DsRadioGroup,
  DsDatePicker,
  DsDateRangePicker,
  DsModalForm,
  DsFormSection,
  DsFormGrid,
  DsFormActions,
  DsReadonlyField,
  DsPageHeader,
  DsHeaderActions,
  DsTopBar,
  DsDeptTreeSelect,
  DsUserAvatarSelect,
  DsRoleMultiSelect,
  DsDictSelect,
  DsStatusSelect,
  DsCascaderSelect,
  DsFileCategorySelect,
  DsFilterBar,
  DsAdvancedFilterPanel,
  DsKeywordSearch,
  DsStatusTag,
  DsRoleTag,
  DsTag,
  DsUserAvatar,
  DsPriorityStars,
  DsEmptyState,
  DsSecurityPopover,
  DsSecurityStatus,
  DsStatsCard,
  DsSecuritySummaryCard,
  DsActionBar,
  DsBatchActions,
  DsIconButton,
  DsDataTable,
  DsUserCell,
  DsPagination
]

const install = (app) => {
  components.forEach((component) => app.component(component.name || component.__name, component))
}

export {
  DsInput,
  DsPasswordInput,
  DsNumberInput,
  DsAmountInput,
  DsTextarea,
  DsRichTextEditor,
  DsSwitch,
  DsCheckboxGroup,
  DsRadioGroup,
  DsDatePicker,
  DsDateRangePicker,
  DsModalForm,
  DsFormSection,
  DsFormGrid,
  DsFormActions,
  DsReadonlyField,
  DsPageHeader,
  DsHeaderActions,
  DsTopBar,
  DsDeptTreeSelect,
  DsUserAvatarSelect,
  DsRoleMultiSelect,
  DsDictSelect,
  DsStatusSelect,
  DsCascaderSelect,
  DsFileCategorySelect,
  DsFilterBar,
  DsAdvancedFilterPanel,
  DsKeywordSearch,
  DsStatusTag,
  DsRoleTag,
  DsTag,
  DsUserAvatar,
  DsPriorityStars,
  DsEmptyState,
  DsSecurityPopover,
  DsSecurityStatus,
  DsStatsCard,
  DsSecuritySummaryCard,
  DsActionBar,
  DsBatchActions,
  DsIconButton,
  DsDataTable,
  DsUserCell,
  DsPagination
}

export { DS_INPUT_TYPES, normalizeDsInputValue, validateDsInputValue }

export default { install }
