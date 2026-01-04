import React, { useState, useRef, useEffect } from 'react'

function Tooltip({ content, children, position = 'top', id, className = '' }) {
  const [isVisible, setIsVisible] = useState(false)
  const [tooltipPosition, setTooltipPosition] = useState({ top: 0, left: 0 })
  const tooltipRef = useRef(null)
  const triggerRef = useRef(null)

      useEffect(() => {
    if (isVisible && tooltipRef.current && triggerRef.current) {
      // Use setTimeout to ensure tooltip is rendered before calculating position
      const timer = setTimeout(() => {
        if (tooltipRef.current && triggerRef.current) {
          const triggerRect = triggerRef.current.getBoundingClientRect()
          const tooltipRect = tooltipRef.current.getBoundingClientRect()

          let top = 0
          let left = 0

          switch (position) {
            case 'top':
              top = triggerRect.top - tooltipRect.height - 8
              left = triggerRect.left + (triggerRect.width / 2) - (tooltipRect.width / 2)
              break
            case 'bottom':
              top = triggerRect.bottom + 8
              left = triggerRect.left + (triggerRect.width / 2) - (tooltipRect.width / 2)
              break
            case 'left':
              top = triggerRect.top + (triggerRect.height / 2) - (tooltipRect.height / 2)
              left = triggerRect.left - tooltipRect.width - 8
              break
            case 'right':
              top = triggerRect.top + (triggerRect.height / 2) - (tooltipRect.height / 2)
              left = triggerRect.right + 8
              break
            default:
              top = triggerRect.top - tooltipRect.height - 8
              left = triggerRect.left + (triggerRect.width / 2) - (tooltipRect.width / 2)
          }

          // Keep tooltip within viewport
          const viewportWidth = window.innerWidth
          const viewportHeight = window.innerHeight

          if (left < 8) left = 8
          if (left + tooltipRect.width > viewportWidth - 8) {
            left = viewportWidth - tooltipRect.width - 8
          }
          if (top < 8) top = 8
          if (top + tooltipRect.height > viewportHeight - 8) {
            top = viewportHeight - tooltipRect.height - 8
          }

          setTooltipPosition({ top, left })
        }
      }, 0)

      return () => clearTimeout(timer)
    }
  }, [isVisible, position])

  const handleMouseEnter = () => {
    console.log('Tooltip mouse enter', { id, hasContent: !!content })
    setIsVisible(true)
  }

  const handleMouseLeave = () => {
    console.log('Tooltip mouse leave', { id })
    setIsVisible(false)
  }

  if (!content) {
    return children
  }

  return (
    <>
      <div
        ref={triggerRef}
        id={id}
        className={`tooltip-trigger ${className}`}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        style={{ position: 'relative', display: 'block', width: '100%', height: '100%', cursor: content ? 'help' : 'default' }}
      >
        {children}
      </div>
      {isVisible && content && (
        <div
          ref={tooltipRef}
          className="tooltip"
          id={`tooltip-${id || 'default'}`}
          style={{
            position: 'fixed',
            top: `${tooltipPosition.top}px`,
            left: `${tooltipPosition.left}px`,
            zIndex: 10000,
            pointerEvents: 'auto',
          }}
          onMouseEnter={handleMouseEnter}
          onMouseLeave={handleMouseLeave}
        >
          {content}
        </div>
      )}
    </>
  )
}

export default Tooltip

